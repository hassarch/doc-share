package com.docshare.backend.auth.controller;

import com.docshare.backend.auth.dto.LoginRequest;
import com.docshare.backend.auth.dto.LogoutRequest;
import com.docshare.backend.auth.dto.PasswordResetConfirmRequest;
import com.docshare.backend.auth.dto.PasswordResetRequest;
import com.docshare.backend.auth.dto.RefreshRequest;
import com.docshare.backend.auth.dto.RegisterRequest;
import com.docshare.backend.auth.dto.TokenPairResponse;
import com.docshare.backend.auth.dto.UserResponse;
import com.docshare.backend.auth.service.AuthenticationService;
import com.docshare.backend.users.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * All endpoints here are on {@code /api/v1/auth/**}, which {@code SecurityConfig}'s {@code
 * PUBLIC_PATHS} allowlist exempts from authentication (FR-1.7 — everything else requires a valid
 * JWT).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthenticationService authenticationService;

  public AuthController(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
    User user = authenticationService.register(request.email(), request.password(), request.name());
    UserResponse response = new UserResponse(user.getId(), user.getEmail(), user.getName());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/login")
  public ResponseEntity<TokenPairResponse> login(@Valid @RequestBody LoginRequest request) {
    TokenPairResponse tokens = authenticationService.login(request.email(), request.password());
    return ResponseEntity.ok(tokens);
  }

  @PostMapping("/refresh")
  public ResponseEntity<TokenPairResponse> refresh(@Valid @RequestBody RefreshRequest request) {
    TokenPairResponse tokens = authenticationService.refresh(request.refreshToken());
    return ResponseEntity.ok(tokens);
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
    authenticationService.logout(request.refreshToken());
    return ResponseEntity.noContent().build();
  }

  /**
   * Always returns 202, whether or not the email is registered — see {@link
   * AuthenticationService#requestPasswordReset(String)} for the anti-enumeration reasoning.
   */
  @PostMapping("/password-reset/request")
  public ResponseEntity<Void> requestPasswordReset(
      @Valid @RequestBody PasswordResetRequest request) {
    authenticationService.requestPasswordReset(request.email());
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/password-reset/confirm")
  public ResponseEntity<Void> confirmPasswordReset(
      @Valid @RequestBody PasswordResetConfirmRequest request) {
    authenticationService.confirmPasswordReset(request.token(), request.newPassword());
    return ResponseEntity.ok().build();
  }
}
