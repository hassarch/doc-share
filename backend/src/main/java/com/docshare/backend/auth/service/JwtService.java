package com.docshare.backend.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * JWT generation and validation. Access tokens are short-lived (15 min), self-contained signed
 * tokens — no database lookup needed to validate them, which is the entire horizontal-scaling
 * benefit of JWTs. The tradeoff is revocation: once issued, they're valid until expiration, so
 * logout (FR-1.3) works by revoking the refresh token (which is stateful, stored in Redis), then
 * accepting that the old access token remains valid for up to 15 more minutes — a deliberate,
 * documented window.
 */
@Service
public class JwtService {

  private static final Logger log = LoggerFactory.getLogger(JwtService.class);

  private final SecretKey secretKey;
  private final long accessTokenTtlMillis;

  public JwtService(
      @Value("${docshare.jwt.secret}") String secret,
      @Value("${docshare.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    this.accessTokenTtlMillis = Duration.ofMinutes(accessTokenTtlMinutes).toMillis();
  }

  /**
   * Generates a signed JWT containing the user's ID and email. The token is valid for 15 minutes
   * (configurable). No database state is involved — the signature proves authenticity.
   */
  public String generateAccessToken(UUID userId, String email) {
    Date now = new Date();
    Date expiration = new Date(now.getTime() + accessTokenTtlMillis);

    return Jwts.builder()
        .subject(userId.toString())
        .claim("email", email)
        .issuedAt(now)
        .expiration(expiration)
        .signWith(secretKey)
        .compact();
  }

  /**
   * Validates the JWT signature and expiration, returning the claims if valid. Returns null if the
   * token is malformed, expired, or has an invalid signature — caller should treat this as
   * unauthorized.
   */
  public Claims validateToken(String token) {
    try {
      return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    } catch (ExpiredJwtException e) {
      log.debug("JWT token expired: {}", e.getMessage());
      return null;
    } catch (SignatureException e) {
      log.warn("Invalid JWT signature: {}", e.getMessage());
      return null;
    } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
      log.warn("Invalid JWT token: {}", e.getMessage());
      return null;
    }
  }

  /** Extracts user ID from a validated token's claims. */
  public UUID getUserIdFromClaims(Claims claims) {
    return UUID.fromString(claims.getSubject());
  }

  /** Extracts email from a validated token's claims. */
  public String getEmailFromClaims(Claims claims) {
    return claims.get("email", String.class);
  }
}
