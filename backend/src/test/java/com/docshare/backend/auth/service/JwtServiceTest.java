package com.docshare.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.docshare.backend.config.JwtProperties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    JwtProperties properties = new JwtProperties();
    properties.setSecret("test-only-secret-key-must-be-at-least-32-bytes-long-for-hs256");
    properties.setAccessTokenTtlMinutes(15);
    properties.setRefreshTokenTtlDays(7);
    jwtService = new JwtService(properties);
  }

  @Test
  void issuedTokenValidatesBackToTheSameUserId() {
    UUID userId = UUID.randomUUID();

    String token = jwtService.issueAccessToken(userId, "person@docshare.local");
    UUID resolved = jwtService.validateAndGetUserId(token);

    assertThat(resolved).isEqualTo(userId);
  }

  @Test
  void tamperedTokenFailsValidation() {
    UUID userId = UUID.randomUUID();
    String token = jwtService.issueAccessToken(userId, "person@docshare.local");
    String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

    assertThatThrownBy(() -> jwtService.validateAndGetUserId(tampered))
        .isInstanceOf(JwtService.JwtValidationException.class);
  }

  @Test
  void garbageInputFailsValidation() {
    assertThatThrownBy(() -> jwtService.validateAndGetUserId("not-a-jwt-at-all"))
        .isInstanceOf(JwtService.JwtValidationException.class);
  }
}
