package com.docshare.backend;

import org.junit.jupiter.api.Test;

/**
 * Smoke test: fails fast if the Spring context can't wire up (missing bean, bad config property,
 * circular dependency, etc). This is intentionally the first test in the project — every phase from
 * here on should keep this passing.
 *
 * <p>Now runs against real Testcontainers infrastructure (Postgres+Redis) rather than excluding
 * autoconfiguration, since Phase 4+ added repositories that require a real DataSource, and Phase 5+
 * adds services that require Redis.
 */
class BackendApplicationTests extends AbstractIntegrationTest {

  @Test
  void contextLoads() {}
}
