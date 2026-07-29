package com.docshare.backend.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.docshare.backend.AbstractIntegrationTest;
import com.docshare.backend.auth.dto.LoginRequest;
import com.docshare.backend.auth.dto.RegisterRequest;
import com.docshare.backend.users.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration test for the authentication flow (FR-1.1-1.7): registration, login, refresh, logout,
 * and password reset. Runs against real Postgres and Redis via {@link AbstractIntegrationTest}.
 *
 * <p>Verifies that JWTs are issued and validated correctly, refresh tokens are stored in Redis, and
 * logout revokes the refresh token.
 */
@AutoConfigureMockMvc
class AuthControllerIT extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private StringRedisTemplate redisTemplate;

  @AfterEach
  void cleanup() {
    userRepository.deleteAll();
    // Clear all Redis keys with our prefixes
    var keys = redisTemplate.keys("refresh_token:*");
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
    }
    keys = redisTemplate.keys("password_reset:*");
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
    }
  }

  @Test
  void registerCreatesUserAndReturnsTokens() throws Exception {
    RegisterRequest request = new RegisterRequest("ada@docshare.local", "password123", "Ada Lovelace");

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn();

    // Verify user was created in the database
    var user = userRepository.findByEmail("ada@docshare.local");
    assertThat(user).isPresent();
    assertThat(user.get().getName()).isEqualTo("Ada Lovelace");
    assertThat(user.get().getStorageQuotaBytes()).isEqualTo(5_368_709_120L); // 5 GB

    // Verify refresh token was stored in Redis
    String responseBody = result.getResponse().getContentAsString();
    String refreshToken =
        objectMapper.readTree(responseBody).get("refreshToken").asText();
    String redisKey = "refresh_token:" + refreshToken;
    assertThat(redisTemplate.hasKey(redisKey)).isTrue();
  }

  @Test
  void registerRejectsDuplicateEmail() throws Exception {
    RegisterRequest request = new RegisterRequest("ada@docshare.local", "password123", "Ada Lovelace");

    // First registration succeeds
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    // Second registration with same email fails
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors[0].message").value("Email already registered"));
  }

  @Test
  void loginAuthenticatesUserAndReturnsTokens() throws Exception {
    // Register a user first
    RegisterRequest registerRequest =
        new RegisterRequest("ada@docshare.local", "password123", "Ada Lovelace");
    mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerRequest)));

    // Login with same credentials
    LoginRequest loginRequest = new LoginRequest("ada@docshare.local", "password123");
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty());
  }

  @Test
  void loginRejectsInvalidCredentials() throws Exception {
    // Register a user first
    RegisterRequest registerRequest =
        new RegisterRequest("ada@docshare.local", "password123", "Ada Lovelace");
    mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerRequest)));

    // Login with wrong password
    LoginRequest loginRequest = new LoginRequest("ada@docshare.local", "wrongpassword");
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].message").value("Invalid email or password"));
  }

  @Test
  void refreshIssuesNewAccessToken() throws Exception {
    // Register and get tokens
    RegisterRequest registerRequest =
        new RegisterRequest("ada@docshare.local", "password123", "Ada Lovelace");
    MvcResult registerResult =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
            .andReturn();

    String responseBody = registerResult.getResponse().getContentAsString();
    String originalAccessToken =
        objectMapper.readTree(responseBody).get("accessToken").asText();
    String refreshToken =
        objectMapper.readTree(responseBody).get("refreshToken").asText();

    // Use refresh token to get a new access token
    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"refreshToken\":\"%s\"}", refreshToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").value(refreshToken)); // Same refresh token
  }

  @Test
  void logoutRevokesRefreshToken() throws Exception {
    // Register and get tokens
    RegisterRequest registerRequest =
        new RegisterRequest("ada@docshare.local", "password123", "Ada Lovelace");
    MvcResult registerResult =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
            .andReturn();

    String responseBody = registerResult.getResponse().getContentAsString();
    String refreshToken =
        objectMapper.readTree(responseBody).get("refreshToken").asText();

    // Logout
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"refreshToken\":\"%s\"}", refreshToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Logged out successfully"));

    // Verify refresh token was removed from Redis
    String redisKey = "refresh_token:" + refreshToken;
    assertThat(redisTemplate.hasKey(redisKey)).isFalse();

    // Attempt to use the revoked refresh token should fail
    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"refreshToken\":\"%s\"}", refreshToken)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].message").value("Invalid or expired refresh token"));
  }

  @Test
  void passwordResetReturnsGenericResponse() throws Exception {
    // Password reset always returns 200, even for non-existent email (anti-enumeration)
    mockMvc
        .perform(
            post("/api/v1/auth/password-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody@docshare.local\"}"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.message")
                .value("If that email is registered, a password reset link has been sent"));

    // Register a user
    RegisterRequest registerRequest =
        new RegisterRequest("ada@docshare.local", "password123", "Ada Lovelace");
    mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerRequest)));

    // Password reset for registered user also returns 200 with same message
    mockMvc
        .perform(
            post("/api/v1/auth/password-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ada@docshare.local\"}"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.message")
                .value("If that email is registered, a password reset link has been sent"));

    // Reset token should be in Redis (but we don't expose it via API for security)
    var resetKeys = redisTemplate.keys("password_reset:*");
    assertThat(resetKeys).hasSize(1);
  }
}
