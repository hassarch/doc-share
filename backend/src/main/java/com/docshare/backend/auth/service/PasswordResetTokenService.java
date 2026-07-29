package com.docshare.backend.auth.service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Manages time-limited password-reset tokens (FR-1.4), stored in Redis the same way refresh tokens
 * are — opaque random string, TTL-bound, no decoding required to check validity.
 *
 * <p>Tokens are single-use: {@link #consume(String)} looks up and deletes in one call, so a token
 * can't be replayed after a successful reset.
 */
@Service
public class PasswordResetTokenService {

  private static final String KEY_PREFIX = "password_reset:";

  private final StringRedisTemplate redisTemplate;
  private final Duration tokenTtl;

  public PasswordResetTokenService(
      StringRedisTemplate redisTemplate,
      @Value("${docshare.password-reset.token-ttl-minutes}") long tokenTtlMinutes) {
    this.redisTemplate = redisTemplate;
    this.tokenTtl = Duration.ofMinutes(tokenTtlMinutes);
  }

  public String issue(UUID userId) {
    String token = UUID.randomUUID().toString();
    redisTemplate.opsForValue().set(KEY_PREFIX + token, userId.toString(), tokenTtl);
    return token;
  }

  /** Looks up and immediately deletes the token — single-use by design. */
  public Optional<UUID> consume(String token) {
    String key = KEY_PREFIX + token;
    String value = redisTemplate.opsForValue().get(key);
    if (value == null) {
      return Optional.empty();
    }
    redisTemplate.delete(key);
    return Optional.of(UUID.fromString(value));
  }
}
