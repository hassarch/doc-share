package com.docshare.backend.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.docshare.backend.AbstractPostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Exercises the real HTTP endpoints, through real Postgres and real Redis (via {@link
 * AbstractPostgresIntegrationTest}) — proving registration, login, refresh rotation, and logout
 * revocation all actually work together, not just in isolation.
 */
class AuthControllerIT extends AbstractPostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void registerThenLoginThenRefreshThenLogout() throws Exception {
    String email = "flow-test-" + System.nanoTime() + "@docshare.local";

    // Register
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"correct-horse-battery-staple","name":"Flow Test"}
                    """
                        .formatted(email)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value(email));

    // Login
    MvcResult loginResult =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"%s","password":"correct-horse-battery-staple"}
                        """
                            .formatted(email)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken", notNullValue()))
            .andExpect(jsonPath("$.refreshToken", notNullValue()))
            .andReturn();

    var loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
    String refreshToken = loginBody.get("refreshToken").asText();

    // Refresh — old token rotates to a new one
    MvcResult refreshResult =
        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"refreshToken":"%s"}
                        """
                            .formatted(refreshToken)))
            .andExpect(status().isOk())
            .andReturn();

    var refreshBody = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
    String newRefreshToken = refreshBody.get("refreshToken").asText();
    assertThat(newRefreshToken).isNotEqualTo(refreshToken);

    // Old refresh token is now dead (rotation) — reusing it must fail
    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"refreshToken":"%s"}
                    """
                        .formatted(refreshToken)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));

    // Logout revokes the current refresh token
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"refreshToken":"%s"}
                    """
                        .formatted(newRefreshToken)))
        .andExpect(status().isNoContent());

    // ...and using it again after logout must fail
    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"refreshToken":"%s"}
                    """
                        .formatted(newRefreshToken)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginWithWrongPasswordReturns401WithGenericMessage() throws Exception {
    String email = "wrong-pw-test-" + System.nanoTime() + "@docshare.local";

    mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"email":"%s","password":"correct-horse-battery-staple","name":"Test"}
                """
                    .formatted(email)));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"totally-wrong-password"}
                    """
                        .formatted(email)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
        .andExpect(jsonPath("$.error.message").value("Invalid email or password"));
  }

  @Test
  void registeringTheSameEmailTwiceReturns409() throws Exception {
    String email = "dup-test-" + System.nanoTime() + "@docshare.local";
    String body =
        """
        {"email":"%s","password":"correct-horse-battery-staple","name":"Test"}
        """
            .formatted(email);

    mockMvc
        .perform(
            post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("CONFLICT"));
  }

  @Test
  void protectedEndpointRejectsRequestWithNoToken() throws Exception {
    mockMvc.perform(post("/api/v1/documents/upload")).andExpect(status().isUnauthorized());
  }
}
