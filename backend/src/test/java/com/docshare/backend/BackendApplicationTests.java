package com.docshare.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: fails fast if the Spring context can't wire up (missing bean, bad config property,
 * circular dependency, etc). This is intentionally the first test in the project — every phase from
 * here on should keep this passing.
 */
@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

  @Test
  void contextLoads() {}
}
