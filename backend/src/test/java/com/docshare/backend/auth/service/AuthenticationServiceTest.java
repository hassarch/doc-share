package com.docshare.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.docshare.backend.auth.dto.TokenPairResponse;
import com.docshare.backend.common.exception.InvalidCredentialsException;
import com.docshare.backend.users.entity.User;
import com.docshare.backend.users.service.UserService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

  @Mock private UserService userService;
  @Mock private JwtService jwtService;
  @Mock private RefreshTokenService refreshTokenService;
  @Mock private PasswordResetTokenService passwordResetTokenService;

  private AuthenticationService authenticationService;

  @BeforeEach
  void setUp() {
    authenticationService =
        new AuthenticationService(
            userService, jwtService, refreshTokenService, passwordResetTokenService);
  }

  @Test
  void loginWithCorrectCredentialsIssuesTokenPair() {
    User user = new User("a@b.com", "hashed", "Name", 1000L);
    when(userService.findByEmail("a@b.com")).thenReturn(Optional.of(user));
    when(userService.matchesPassword(user, "correct")).thenReturn(true);
    when(jwtService.issueAccessToken(any(), any())).thenReturn("access-token");
    when(refreshTokenService.issue(any())).thenReturn("refresh-token");
    when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);

    TokenPairResponse result = authenticationService.login("a@b.com", "correct");

    assertThat(result.accessToken()).isEqualTo("access-token");
    assertThat(result.refreshToken()).isEqualTo("refresh-token");
    assertThat(result.expiresInSeconds()).isEqualTo(900L);
  }

  @Test
  void loginWithWrongPasswordThrowsInvalidCredentials() {
    User user = new User("a@b.com", "hashed", "Name", 1000L);
    when(userService.findByEmail("a@b.com")).thenReturn(Optional.of(user));
    when(userService.matchesPassword(user, "wrong")).thenReturn(false);

    assertThatThrownBy(() -> authenticationService.login("a@b.com", "wrong"))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Invalid email or password");
  }

  @Test
  void loginWithUnknownEmailThrowsInvalidCredentialsNotNotFound() {
    when(userService.findByEmail("nobody@b.com")).thenReturn(Optional.empty());

    // Deliberately the same exception/message as "wrong password" -
    // distinguishing the two would let an attacker enumerate emails via
    // this endpoint. This test locks that behavior in.
    assertThatThrownBy(() -> authenticationService.login("nobody@b.com", "anything"))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Invalid email or password");
  }

  @Test
  void refreshWithValidTokenRotatesAndIssuesNewPair() {
    UUID userId = UUID.randomUUID();
    User user = new User("a@b.com", "hashed", "Name", 1000L);
    when(refreshTokenService.resolveUserId("old-token")).thenReturn(Optional.of(userId));
    when(userService.findById(userId)).thenReturn(Optional.of(user));
    when(jwtService.issueAccessToken(any(), any())).thenReturn("new-access");
    when(refreshTokenService.issue(any())).thenReturn("new-refresh");

    TokenPairResponse result = authenticationService.refresh("old-token");

    assertThat(result.accessToken()).isEqualTo("new-access");
    assertThat(result.refreshToken()).isEqualTo("new-refresh");
    verify(refreshTokenService).revoke("old-token"); // old token must be dead after rotation
  }

  @Test
  void refreshWithUnknownTokenThrowsInvalidCredentials() {
    when(refreshTokenService.resolveUserId("bogus")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authenticationService.refresh("bogus"))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void logoutRevokesTheGivenRefreshToken() {
    authenticationService.logout("some-token");

    verify(refreshTokenService).revoke("some-token");
  }

  @Test
  void requestPasswordResetIssuesTokenWhenEmailExists() {
    User user = new User("a@b.com", "hashed", "Name", 1000L);
    when(userService.findByEmail("a@b.com")).thenReturn(Optional.of(user));

    authenticationService.requestPasswordReset("a@b.com");

    verify(passwordResetTokenService).issue(user.getId());
  }

  @Test
  void requestPasswordResetDoesNothingObservableWhenEmailUnknown() {
    when(userService.findByEmail("nobody@b.com")).thenReturn(Optional.empty());

    // Must not throw, and must not issue a token for a user that doesn't
    // exist - the anti-enumeration contract this method promises.
    authenticationService.requestPasswordReset("nobody@b.com");

    verify(passwordResetTokenService, never()).issue(any());
  }

  @Test
  void confirmPasswordResetChangesPasswordForValidToken() {
    UUID userId = UUID.randomUUID();
    User user = new User("a@b.com", "old-hash", "Name", 1000L);
    when(passwordResetTokenService.consume("reset-token")).thenReturn(Optional.of(userId));
    when(userService.findById(userId)).thenReturn(Optional.of(user));

    authenticationService.confirmPasswordReset("reset-token", "new-password");

    verify(userService).changePassword(user, "new-password");
  }

  @Test
  void confirmPasswordResetWithInvalidTokenThrows() {
    when(passwordResetTokenService.consume("bogus")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authenticationService.confirmPasswordReset("bogus", "new-password"))
        .isInstanceOf(InvalidCredentialsException.class);

    verify(userService, never()).changePassword(any(), any());
  }
}
