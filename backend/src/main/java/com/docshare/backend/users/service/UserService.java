package com.docshare.backend.users.service;

import com.docshare.backend.users.entity.User;
import java.util.Optional;
import java.util.UUID;

/**
 * The only way other modules (notably {@code auth}) are allowed to touch user data — never through
 * {@code users.repository} directly, per ADR-0001 #2. Keeping this interface separate from {@link
 * UserServiceImpl} also means a future extraction of {@code users} into its own service (Phase 3+,
 * per the PRD) only requires swapping the implementation for an HTTP client — callers depending on
 * this interface don't change.
 */
public interface UserService {

  User register(String email, String rawPassword, String name);

  Optional<User> findByEmail(String email);

  Optional<User> findById(UUID id);

  boolean existsByEmail(String email);

  boolean matchesPassword(User user, String rawPassword);

  void changePassword(User user, String newRawPassword);
}
