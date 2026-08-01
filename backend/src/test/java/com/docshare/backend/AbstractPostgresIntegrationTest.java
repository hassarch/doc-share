package com.docshare.backend;

import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests that need real infrastructure — Postgres, Redis (Authentication
 * phase), and now MinIO (Backend APIs phase, for document upload/download). Not H2, not a mock, not
 * an excluded-autoconfiguration "fast" context: this system leans on Postgres-specific features
 * (generated {@code tsvector} columns, JSONB), on Redis actually being reachable for {@code
 * StringRedisTemplate}-backed services, and now on a real S3-compatible endpoint for {@code
 * StorageService}.
 *
 * <p><strong>History note:</strong> {@code BackendApplicationTests} used to run against a {@code
 * test} profile that excluded DataSource/JPA/Redis/ Kafka autoconfiguration, on the theory that a
 * plain "does the context load" smoke test shouldn't need real infrastructure. That broke the
 * moment real {@code @Repository} interfaces existed (Phase 4) and would have broken outright once
 * a bean required {@code StringRedisTemplate} (Authentication phase) — Spring fails fast when a
 * required bean type has no provider. This class replaces that approach: every context-loading
 * test, including the plain smoke test, now runs against real containers, same as every other
 * integration test in this suite.
 *
 * <p>Containers are static, so Testcontainers reuses them across all subclasses' test methods
 * within a single JVM run instead of paying startup cost per test class.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
public abstract class AbstractPostgresIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("docshare_test")
          .withUsername("docshare_test")
          .withPassword("docshare_test")
          .withReuse(true) // Reuse containers - critical for shared Spring context
          .withStartupTimeout(Duration.ofMinutes(2))
          .waitingFor(
              Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 2));

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
          .withExposedPorts(6379)
          .withReuse(true)
          .withStartupTimeout(Duration.ofMinutes(1))
          .waitingFor(Wait.forListeningPort());

  @Container
  static final GenericContainer<?> MINIO =
      new GenericContainer<>(DockerImageName.parse("minio/minio:RELEASE.2024-10-13T13-34-11Z"))
          .withCommand("server", "/data")
          .withEnv("MINIO_ROOT_USER", "docshare_test")
          .withEnv("MINIO_ROOT_PASSWORD", "docshare_test")
          .withExposedPorts(9000)
          .withReuse(true)
          .withStartupTimeout(Duration.ofMinutes(1))
          .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

  @Container
  static final KafkaContainer KAFKA =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"))
          .withReuse(true)
          .withStartupTimeout(Duration.ofMinutes(2));

  @DynamicPropertySource
  static void registerContainerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    // Flyway runs its real migrations (including V1__initial_schema.sql)
    // against this container — this is deliberate: the whole point of
    // these tests is proving the migration + entity mapping actually agree.

    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

    registry.add(
        "docshare.storage.minio.endpoint",
        () -> "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
    registry.add("docshare.storage.minio.access-key", () -> "docshare_test");
    registry.add("docshare.storage.minio.secret-key", () -> "docshare_test");
    registry.add("docshare.storage.bucket", () -> "documents-test");

    registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
  }
}
