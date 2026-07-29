package com.docshare.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared base class for integration tests, providing Testcontainers-managed Postgres and Redis
 * instances. All tests that need a full application context (including repository/service layer
 * tests and context-loading smoke tests) should extend this, so they run against real
 * infrastructure instead of mocks or excluded autoconfiguration.
 *
 * <p>Containers are marked reusable via {@code testcontainers.reuse.enable=true} in
 * testcontainers.properties, so they persist across test runs to speed up local development — but
 * each test still sees a clean state via transactional rollback or explicit cleanup.
 */
@SpringBootTest
@Testcontainers
public abstract class AbstractIntegrationTest {

  private static final PostgreSQLContainer<?> POSTGRES_CONTAINER;
  private static final GenericContainer<?> REDIS_CONTAINER;

  static {
    POSTGRES_CONTAINER =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("docshare_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);
    POSTGRES_CONTAINER.start();

    REDIS_CONTAINER =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);
    REDIS_CONTAINER.start();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // Postgres
    registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
    registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);

    // Redis
    registry.add(
        "spring.data.redis.host",
        () -> REDIS_CONTAINER.getHost());
    registry.add(
        "spring.data.redis.port",
        () -> REDIS_CONTAINER.getMappedPort(6379).toString());
  }
}
