package com.docshare.backend.users.service;

import com.docshare.backend.common.exception.ConflictException;
import com.docshare.backend.users.entity.User;
import com.docshare.backend.users.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

  private static final long DEFAULT_STORAGE_QUOTA_BYTES = 5L * 1024 * 1024 * 1024; // 5 GB

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public User register(String email, String rawPassword, String name) {
    if (userRepository.existsByEmail(email)) {
      // Deliberately generic — doesn't distinguish "email taken" from other
      // failures in the response the frontend would show, though the
      // exception code (CONFLICT) does let the frontend show a specific
      // "this email is already registered" message, which is acceptable
      // here (unlike login) since registration inherently confirms
      // whether you, the person filling out the form, already have an
      // account with that email — there's no meaningful enumeration risk
      // being introduced beyond what the form itself implies.
      throw new ConflictException("An account with this email already exists");
    }
    User user =
        new User(email, passwordEncoder.encode(rawPassword), name, DEFAULT_STORAGE_QUOTA_BYTES);
    return userRepository.save(user);
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  @Override
  public Optional<User> findById(UUID id) {
    return userRepository.findById(id);
  }

  @Override
  public boolean existsByEmail(String email) {
    return userRepository.existsByEmail(email);
  }

  @Override
  public boolean matchesPassword(User user, String rawPassword) {
    return passwordEncoder.matches(rawPassword, user.getPasswordHash());
  }

  @Override
  @Transactional
  public void changePassword(User user, String newRawPassword) {
    user.updatePasswordHash(passwordEncoder.encode(newRawPassword));
    userRepository.save(user);
  }
}
