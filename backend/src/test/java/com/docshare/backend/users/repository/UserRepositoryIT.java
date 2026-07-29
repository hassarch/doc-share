package com.docshare.backend.users.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.docshare.backend.AbstractPostgresIntegrationTest;
import com.docshare.backend.users.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proves the full chain works together: Flyway's V1 migration creates the {@code users} table,
 * Hibernate's {@code User} entity mapping matches it (recall {@code ddl-auto: validate} — this test
 * would fail loudly at context startup if the entity and the migration disagreed), and the
 * repository can actually read/write through it against a real Postgres.
 *
 * <p>Uses Testcontainers-managed Postgres and Redis via {@link AbstractPostgresIntegrationTest}.
 */
@Transactional
class UserRepositoryIT extends AbstractPostgresIntegrationTest {

  @Autowired private UserRepository userRepository;

  @AfterEach
  void cleanup() {
    userRepository.deleteAll();
  }

  @Test
  void savesAndFindsUserByEmail() {
    User user =
        new User("ada@docshare.local", "bcrypt-hash-placeholder", "Ada Lovelace", 5_368_709_120L);

    userRepository.save(user);

    var found = userRepository.findByEmail("ada@docshare.local");

    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Ada Lovelace");
    assertThat(found.get().getStorageUsedBytes()).isZero();
    // Populated by JpaAuditingConfig, not set manually — proves auditing
    // is actually wired up, not just present in code.
    assertThat(found.get().getCreatedAt()).isNotNull();
    assertThat(found.get().getUpdatedAt()).isNotNull();
  }

  @Test
  void existsByEmailReflectsSavedUsers() {
    assertThat(userRepository.existsByEmail("nobody@docshare.local")).isFalse();

    userRepository.save(new User("nobody@docshare.local", "hash", "Nobody", 1_000_000_000L));

    assertThat(userRepository.existsByEmail("nobody@docshare.local")).isTrue();
  }

  @Test
  void recordStorageUsedRejectsNegativeResult() {
    User user = new User("quota@docshare.local", "hash", "Quota Test", 1000L);

    user.recordStorageUsed(500L);
    assertThat(user.getStorageUsedBytes()).isEqualTo(500L);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> user.recordStorageUsed(-600L))
        .isInstanceOf(IllegalStateException.class);
  }
}
