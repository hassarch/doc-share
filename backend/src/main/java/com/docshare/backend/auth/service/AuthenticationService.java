package com.docshare.backend.auth.service;

import com.docshare.backend.auth.dto.AuthResponse;
import com.docshare.backend.auth.dto.LoginRequest;
import com.docshare.backend.auth.dto.RegisterRequest;
import com.docshare.backend.common.exception.ConflictException;
import com.docshare.backend.common.exception.ValidationException;
import com.docshare.backend.users.entity.User;
import com.docshare.backend.users.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core authentication operations: registration (FR-1.1), login (FR-1.2), refresh (FR-1.6), logout
 * (FR-1.3), and password-reset token issuance (FR-1.4). This is deliberately NOT wired through
 * Spring Security's AuthenticationManager machinery — login is a stateless REST endpoint that
 * returns tokens, not a session-based form flow, so manually comparing the password hash and
 * issuing tokens directly is simpler and more explicit than fighting framework assumptions.
 *
 * <p>Refresh tokens are stored in Redis with a 7-day TTL (configurable). They're opaque random
 * strings, not JWTs — the point is to make them revocable (logout) without blacklisting access
 * tokens.
 */
@Service
public class AuthenticationService {

  private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final StringRedisTemplate redisTemplate;
  private final SecureRandom secureRandom = new SecureRandom();
  private final long refreshTokenTtlMillis;

  public AuthenticationService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      StringRedisTemplate redisTemplate,
      @Value("${docshare.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.redisTemplate = redisTemplate;
    this.refreshTokenTtlMillis = Duration.ofDays(refreshTokenTtlDays).toMillis();
  }

  /**
   * Registers a new user (FR-1.1). Email is normalized to lowercase; password is bcrypt-hashed.
   * Returns JWT pair immediately — no separate email verification step in this phase.
   */
  @Transactional
  public AuthResponse register(RegisterRequest request) {
    String normalizedEmail = request.email().toLowerCase().trim();

    if (userRepository.existsByEmail(normalizedEmail)) {
      throw new ConflictException("Email already registered");
    }

    String hashedPassword = passwordEncoder.encode(request.password());
    User user =
        new User(
            normalizedEmail,
            hashedPassword,
            request.name().trim(),
            5_368_709_120L // 5 GB default quota per PRD FR-8
            );

    userRepository.save(user);
    log.info("User registered: email={}, userId={}", normalizedEmail, user.getId());

    return issueTokens(user);
  }

  /**
   * Authenticates a user (FR-1.2). Compares the provided password against the stored bcrypt hash.
   * Returns JWT pair on success; throws ValidationException if credentials are invalid.
   */
  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    String normalizedEmail = request.email().toLowerCase().trim();

    User user =
        userRepository
            .findByEmail(normalizedEmail)
            .orElseThrow(() -> new ValidationException("Invalid email or password"));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new ValidationException("Invalid email or password");
    }

    log.info("User logged in: email={}, userId={}", normalizedEmail, user.getId());
    return issueTokens(user);
  }

  /**
   * Issues a new access token using a valid refresh token (FR-1.6). The refresh token must exist
   * in Redis and must belong to the user it claims to belong to.
   */
  @Transactional(readOnly = true)
  public AuthResponse refresh(String refreshToken) {
    String userIdStr = redisTemplate.opsForValue().get(refreshTokenKey(refreshToken));

    if (userIdStr == null) {
      throw new ValidationException("Invalid or expired refresh token");
    }

    UUID userId = UUID.fromString(userIdStr);
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ValidationException("User not found"));

    String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
    log.debug("Access token refreshed for userId={}", userId);

    // Reuse the existing refresh token — no need to rotate it on every refresh
    return new AuthResponse(newAccessToken, refreshToken);
  }

  /**
   * Revokes a refresh token (FR-1.3), making it unusable for future access token generation. The
   * current access token remains valid until expiration (up to 15 min) — this is deliberate, not a
   * bug. Blacklisting JWTs would defeat the horizontal-scaling benefit of stateless tokens.
   */
  public void logout(String refreshToken) {
    String key = refreshTokenKey(refreshToken);
    Boolean deleted = redisTemplate.delete(key);

    if (Boolean.TRUE.equals(deleted)) {
      log.info("Refresh token revoked: {}", refreshToken.substring(0, 8) + "...");
    } else {
      log.debug("Logout called with non-existent or already-expired refresh token");
    }
  }

  /**
   * Initiates password reset (FR-1.4). Always returns success to prevent account enumeration. If
   * the email exists, a reset token is generated and stored in Redis with a 1-hour TTL.
   *
   * <p><strong>TODO(notification-phase):</strong> Once the Notification Service is implemented,
   * this token should be emailed to the user. For now, it's only logged server-side for manual
   * retrieval in local testing.
   */
  @Transactional(readOnly = true)
  public void initiatePasswordReset(String email) {
    String normalizedEmail = email.toLowerCase().trim();
    var user = userRepository.findByEmail(normalizedEmail);

    if (user.isPresent()) {
      String resetToken = generateSecureToken();
      String key = passwordResetTokenKey(resetToken);

      redisTemplate.opsForValue().set(key, user.get().getId().toString(), Duration.ofHours(1));

      // TODO(notification-phase): Send email instead of logging
      log.warn(
          "Password reset token (LOG ONLY — will be emailed once Notification Service is ready): "
              + "email={}, token={}",
          normalizedEmail,
          resetToken);
    } else {
      log.debug("Password reset requested for non-existent email: {}", normalizedEmail);
    }
  }

  /** Generates a JWT pair and stores the refresh token in Redis. */
  private AuthResponse issueTokens(User user) {
    String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
    String refreshToken = generateSecureToken();

    redisTemplate
        .opsForValue()
        .set(refreshTokenKey(refreshToken), user.getId().toString(), Duration.ofMillis(refreshTokenTtlMillis));

    return new AuthResponse(accessToken, refreshToken);
  }

  /** Generates a cryptographically secure random token. */
  private String generateSecureToken() {
    byte[] randomBytes = new byte[32];
    secureRandom.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }

  private String refreshTokenKey(String token) {
    return "refresh_token:" + token;
  }

  private String passwordResetTokenKey(String token) {
    return "password_reset:" + token;
  }
}
