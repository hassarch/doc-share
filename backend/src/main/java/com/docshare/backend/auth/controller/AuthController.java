package com.docshare.backend.auth.controller;

import com.docshare.backend.auth.dto.AuthResponse;
import com.docshare.backend.auth.dto.LoginRequest;
import com.docshare.backend.auth.dto.PasswordResetRequest;
import com.docshare.backend.auth.dto.RefreshRequest;
import com.docshare.backend.auth.dto.RegisterRequest;
import com.docshare.backend.auth.service.AuthenticationService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints: registration, login, token refresh, logout, and password reset
 * initiation. These are the only public (non-JWT-protected) endpoints in the API (FR-1.7), per
 * {@code SecurityConfig.PUBLIC_PATHS}.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthenticationService authenticationService;

  public AuthController(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  /**
   * Registers a new user (FR-1.1). Email is normalized to lowercase; password is bcrypt-hashed.
   * Returns JWT pair immediately — no separate email verification in this phase.
   */
  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    AuthResponse response = authenticationService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Authenticates a user (FR-1.2). Returns JWT pair on success; 400 if credentials are invalid.
   */
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authenticationService.login(request);
    return ResponseEntity.ok(response);
  }

  /**
   * Issues a new access token using a valid refresh token (FR-1.6). Refresh token itself is not
   * rotated — the same one continues to work until its 7-day TTL expires or logout revokes it.
   */
  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
    AuthResponse response = authenticationService.refresh(request.refreshToken());
    return ResponseEntity.ok(response);
  }

  /**
   * Revokes a refresh token (FR-1.3), preventing it from issuing new access tokens. The current
   * access token remains valid until expiration (up to 15 min) — this is deliberate. Blacklisting
   * JWTs would defeat the horizontal-scaling benefit of stateless tokens.
   */
  @PostMapping("/logout")
  public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody RefreshRequest request) {
    authenticationService.logout(request.refreshToken());
    return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
  }

  /**
   * Initiates password reset (FR-1.4). Always returns 200 whether or not the email exists, to
   * prevent account enumeration. If the email is registered, a reset token is generated and logged
   * server-side (will be emailed once Notification Service is implemented).
   */
  @PostMapping("/password-reset")
  public ResponseEntity<Map<String, String>> passwordReset(
      @Valid @RequestBody PasswordResetRequest request) {
    authenticationService.initiatePasswordReset(request.email());
    return ResponseEntity.ok(
        Map.of("message", "If that email is registered, a password reset link has been sent"));
  }
}
