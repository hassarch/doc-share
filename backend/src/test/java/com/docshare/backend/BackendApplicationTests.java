package com.docshare.backend;

import org.junit.jupiter.api.Test;

/**
 * Smoke test: fails fast if the Spring context can't wire up (missing bean, bad config property,
 * circular dependency, etc). Runs against real Testcontainers-backed Postgres and Redis via {@link
 * AbstractPostgresIntegrationTest} — see that class's Javadoc for why the earlier
 * excluded-autoconfiguration approach was retired in the Authentication phase.
 */
class BackendApplicationTests extends AbstractPostgresIntegrationTest {

  @Test
  void contextLoads() {}
}
