package com.docshare.backend.auth.service;

import com.docshare.backend.config.JwtProperties;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Manages refresh tokens: opaque, random, server-tracked strings stored in Redis with a TTL — not
 * JWTs. This is what makes "logout" (FR-1.3) a real operation: revoking access means deleting the
 * one Redis key that could produce new access tokens, rather than trying to un-sign a JWT that's
 * already out in the world.
 *
 * <p>Tokens are rotated on every refresh (the old one is deleted, a new one issued) — a standard
 * defense against a leaked refresh token being reused silently alongside the legitimate client's;
 * if both the attacker and the real client try to use the same (now-consumed) token, the second
 * attempt fails, which is itself a signal something is wrong.
 */
@Service
public class RefreshTokenService {

  private static final String KEY_PREFIX = "refresh_token:";

  private final StringRedisTemplate redisTemplate;
  private final Duration refreshTokenTtl;

  public RefreshTokenService(StringRedisTemplate redisTemplate, JwtProperties properties) {
    this.redisTemplate = redisTemplate;
    this.refreshTokenTtl = Duration.ofDays(properties.getRefreshTokenTtlDays());
  }

  /** Issues a new refresh token for the given user and stores it in Redis. */
  public String issue(UUID userId) {
    String token = UUID.randomUUID().toString();
    redisTemplate.opsForValue().set(KEY_PREFIX + token, userId.toString(), refreshTokenTtl);
    return token;
  }

  /** Looks up the user a refresh token belongs to, if it's still valid. */
  public Optional<UUID> resolveUserId(String token) {
    String value = redisTemplate.opsForValue().get(KEY_PREFIX + token);
    return Optional.ofNullable(value).map(UUID::fromString);
  }

  /** Deletes a refresh token — used both for rotation and for logout. */
  public void revoke(String token) {
    redisTemplate.delete(KEY_PREFIX + token);
  }
}
