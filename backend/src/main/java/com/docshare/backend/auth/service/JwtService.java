package com.docshare.backend.auth.service;

import com.docshare.backend.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and validates short-lived signed JWT access tokens.
 *
 * <p>Deliberately stateless: no database or Redis lookup is needed to validate a token, which is
 * what lets any horizontally-scaled instance validate a request with nothing but the shared signing
 * secret (FR-22.x). The trade-off — a valid token can't be revoked before it expires — is accepted
 * because the TTL is short (15 minutes by default); real revocation happens one layer up, at the
 * refresh-token layer (see {@link RefreshTokenService}), which is what actually gates issuing new
 * access tokens.
 */
@Service
public class JwtService {

  private static final String CLAIM_EMAIL = "email";

  private final SecretKey signingKey;
  private final long accessTokenTtlMinutes;

  public JwtService(JwtProperties properties) {
    this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    this.accessTokenTtlMinutes = properties.getAccessTokenTtlMinutes();
  }

  /** Issues a signed access token for the given user. */
  public String issueAccessToken(UUID userId, String email) {
    Instant now = Instant.now();
    Instant expiry = now.plus(accessTokenTtlMinutes, ChronoUnit.MINUTES);

    return Jwts.builder()
        .subject(userId.toString())
        .claim(CLAIM_EMAIL, email)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .signWith(signingKey)
        .compact();
  }

  public long getAccessTokenTtlSeconds() {
    return accessTokenTtlMinutes * 60;
  }

  /**
   * Parses and validates a token's signature and expiry, returning its subject (the user ID).
   * Throws {@link JwtValidationException} for any failure — expired, malformed, or tampered-with
   * tokens are all indistinguishable to the caller, deliberately, so as not to leak which failure
   * mode occurred.
   */
  public UUID validateAndGetUserId(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
      return UUID.fromString(claims.getSubject());
    } catch (JwtException | IllegalArgumentException e) {
      throw new JwtValidationException("Invalid or expired access token");
    }
  }

  /** Thrown when an access token fails signature verification or has expired. */
  public static class JwtValidationException extends RuntimeException {
    public JwtValidationException(String message) {
      super(message);
    }
  }
}
