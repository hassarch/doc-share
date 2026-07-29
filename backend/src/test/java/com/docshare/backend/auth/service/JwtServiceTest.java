package com.docshare.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test for JWT generation and validation. Uses a fixed secret and TTL for deterministic
 * testing.
 */
class JwtServiceTest {

  private JwtService jwtService;
  private static final String TEST_SECRET = "test-secret-key-for-jwt-signing-minimum-256-bits-required-here";
  private static final long TEST_TTL_MINUTES = 15;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService(TEST_SECRET, TEST_TTL_MINUTES);
  }

  @Test
  void generatesValidAccessToken() {
    UUID userId = UUID.randomUUID();
    String email = "test@docshare.local";

    String token = jwtService.generateAccessToken(userId, email);

    assertThat(token).isNotNull().isNotEmpty();
  }

  @Test
  void validatesAndExtractsClaimsFromValidToken() {
    UUID userId = UUID.randomUUID();
    String email = "test@docshare.local";

    String token = jwtService.generateAccessToken(userId, email);
    Claims claims = jwtService.validateToken(token);

    assertThat(claims).isNotNull();
    assertThat(jwtService.getUserIdFromClaims(claims)).isEqualTo(userId);
    assertThat(jwtService.getEmailFromClaims(claims)).isEqualTo(email);
  }

  @Test
  void rejectsInvalidToken() {
    String invalidToken = "invalid.jwt.token";

    Claims claims = jwtService.validateToken(invalidToken);

    assertThat(claims).isNull();
  }

  @Test
  void rejectsTamperedToken() {
    UUID userId = UUID.randomUUID();
    String email = "test@docshare.local";

    String token = jwtService.generateAccessToken(userId, email);
    // Tamper with the token by changing a character
    String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

    Claims claims = jwtService.validateToken(tamperedToken);

    assertThat(claims).isNull();
  }
}
