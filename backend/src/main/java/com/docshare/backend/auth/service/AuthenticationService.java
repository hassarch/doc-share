package com.docshare.backend.auth.service;

import com.docshare.backend.auth.dto.TokenPairResponse;
import com.docshare.backend.common.exception.InvalidCredentialsException;
import com.docshare.backend.users.entity.User;
import com.docshare.backend.users.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the auth flows (FR-1.1-1.5) by composing {@link UserService} (for identity, via the
 * module boundary — never {@code users.repository} directly), {@link JwtService} (access tokens),
 * {@link RefreshTokenService} (revocable refresh tokens), and {@link PasswordResetTokenService}
 * (reset flow).
 */
@Service
public class AuthenticationService {

  private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

  private final UserService userService;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final PasswordResetTokenService passwordResetTokenService;

  public AuthenticationService(
      UserService userService,
      JwtService jwtService,
      RefreshTokenService refreshTokenService,
      PasswordResetTokenService passwordResetTokenService) {
    this.userService = userService;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
    this.passwordResetTokenService = passwordResetTokenService;
  }

  public User register(String email, String rawPassword, String name) {
    return userService.register(email, rawPassword, name);
  }

  /** FR-1.2: verify credentials, issue an access + refresh token pair. */
  public TokenPairResponse login(String email, String rawPassword) {
    User user =
        userService
            .findByEmail(email)
            .filter(u -> userService.matchesPassword(u, rawPassword))
            .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

    return issueTokenPair(user);
  }

  /**
   * FR-1.5: exchange a valid, unexpired refresh token for a new token pair. The old refresh token
   * is revoked as part of rotation (see {@link RefreshTokenService}).
   */
  public TokenPairResponse refresh(String refreshToken) {
    var userId =
        refreshTokenService
            .resolveUserId(refreshToken)
            .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));

    User user =
        userService
            .findById(userId)
            .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));

    refreshTokenService.revoke(refreshToken); // rotation: old token can't be reused
    return issueTokenPair(user);
  }

  /** FR-1.3: revoke a refresh token — this is what "logout" means server-side. */
  public void logout(String refreshToken) {
    refreshTokenService.revoke(refreshToken);
  }

  /**
   * FR-1.4: issue a password-reset token if the email exists. Deliberately returns nothing
   * distinguishing "email exists" from "email doesn't exist" to the caller — see {@link
   * com.docshare.backend.auth.controller.AuthController} for how the generic response is produced
   * either way.
   *
   * <p>TODO(notification-phase): send the token via email instead of logging it. Logging is a
   * temporary, local-dev-only stand-in until the Notification Service exists — this must not ship
   * to any real environment as-is.
   */
  public void requestPasswordReset(String email) {
    userService
        .findByEmail(email)
        .ifPresent(
            user -> {
              String token = passwordResetTokenService.issue(user.getId());
              log.info(
                  "Password reset requested for userId={}. token={} (TEMPORARY: log instead of"
                      + " email until the Notification Service exists)",
                  user.getId(),
                  token);
            });
  }

  /** FR-1.4: consume a reset token and set the new password. */
  public void confirmPasswordReset(String token, String newRawPassword) {
    var userId =
        passwordResetTokenService
            .consume(token)
            .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired reset token"));

    User user =
        userService
            .findById(userId)
            .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired reset token"));

    userService.changePassword(user, newRawPassword);
  }

  private TokenPairResponse issueTokenPair(User user) {
    String accessToken = jwtService.issueAccessToken(user.getId(), user.getEmail());
    String refreshToken = refreshTokenService.issue(user.getId());
    return new TokenPairResponse(accessToken, refreshToken, jwtService.getAccessTokenTtlSeconds());
  }
}
