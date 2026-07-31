package com.docshare.backend.users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.docshare.backend.common.exception.ConflictException;
import com.docshare.backend.users.entity.User;
import com.docshare.backend.users.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private UserServiceImpl userService;

  @BeforeEach
  void setUp() {
    userService = new UserServiceImpl(userRepository, passwordEncoder);
  }

  @Test
  void registerHashesPasswordAndSavesNewUser() {
    when(userRepository.existsByEmail("ada@docshare.local")).thenReturn(false);
    when(passwordEncoder.encode("plaintext")).thenReturn("hashed");
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User result = userService.register("ada@docshare.local", "plaintext", "Ada");

    assertThat(result.getEmail()).isEqualTo("ada@docshare.local");
    assertThat(result.getPasswordHash()).isEqualTo("hashed");
    verify(passwordEncoder).encode("plaintext");
  }

  @Test
  void registerThrowsConflictWhenEmailAlreadyExists() {
    when(userRepository.existsByEmail("taken@docshare.local")).thenReturn(true);

    assertThatThrownBy(() -> userService.register("taken@docshare.local", "pw", "Name"))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void matchesPasswordDelegatesToPasswordEncoder() {
    User user = new User("a@b.com", "hashed-value", "Name", 1000L);
    when(passwordEncoder.matches("attempt", "hashed-value")).thenReturn(true);

    boolean result = userService.matchesPassword(user, "attempt");

    assertThat(result).isTrue();
  }

  @Test
  void changePasswordEncodesAndPersistsNewHash() {
    User user = new User("a@b.com", "old-hash", "Name", 1000L);
    when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

    userService.changePassword(user, "new-password");

    assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    verify(userRepository).save(user);
  }

  @Test
  void recordStorageUsageAdjustsExistingUser() {
    UUID userId = UUID.randomUUID();
    User user = new User("a@b.com", "hash", "Name", 1000L);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    userService.recordStorageUsage(userId, 500L);

    assertThat(user.getStorageUsedBytes()).isEqualTo(500L);
    verify(userRepository).save(user);
  }

  @Test
  void recordStorageUsageThrowsWhenUserMissing() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.recordStorageUsage(userId, 100L))
        .isInstanceOf(IllegalStateException.class);
  }
}
