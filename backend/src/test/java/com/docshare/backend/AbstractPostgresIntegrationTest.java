package com.docshare.backend;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for repository integration tests that need a real Postgres — not H2, not a mock —
 * because this system leans on Postgres-specific features (generated {@code tsvector} columns,
 * JSONB) that an in-memory substitute can't faithfully emulate.
 *
 * <p>One container is shared across all subclasses' test methods within a single JVM
 * (Testcontainers reuses it across the {@code @SpringBootTest} contexts as long as the container
 * instance itself is static and started once) — this keeps the suite fast as more IT classes are
 * added in later phases, instead of paying container-startup cost per test.
 *
 * <p>Tagged {@code integration} so these can be run separately from the fast unit-test suite in CI
 * (see the CI phase).
 */
@Testcontainers
@SpringBootTest
@Tag("integration")
public abstract class AbstractPostgresIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("docshare_test")
          .withUsername("docshare_test")
          .withPassword("docshare_test");

  @DynamicPropertySource
  static void registerPostgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    // Flyway runs its real migrations (including V1__initial_schema.sql)
    // against this container — this is deliberate: the whole point of
    // these tests is proving the migration + entity mapping actually agree.
  }
}
