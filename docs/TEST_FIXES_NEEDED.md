# Integration Test Fixes

## Issues Identified

### 1. ✅ FIXED: Hardcoded Secrets in health-check.sh
- **Status**: Fixed and committed (f9ce8b9)
- **Solution**: Replaced hardcoded test password with environment variable `HEALTHCHECK_PASSWORD`

### 2. ⚠️  Integration Tests Failing in CI

#### Symptoms
- Tests run but fail assertions
- Example failures:
  - `AuthControllerIT.registeringTheSameEmailTwiceReturns409()` - FAILED at line 158
  - `AuthControllerIT.protectedEndpointRejectsRequestWithNoToken()` - FAILED at line 169
  - `DocumentControllerIT` tests - ALL FAILED at line 52
  - Some `NotificationServiceIT` and `UserRepositoryIT` tests failing with connection errors

#### Root Causes

1. **Test application.yml was missing** ✅ FIXED
   - Created `/backend/src/test/resources/application.yml` with proper test defaults
   - Ensures CORS, JWT, and other required properties are set for tests

2. **Database connection issues in some tests**
   - Some tests show `CannotCreateTransactionException` and `JDBCConnectionException`
   - This suggests Testcontainers might not be fully initialized before tests run

3. **Possible timing issues**
   - Application starts but might not be fully ready when tests execute
   - Flyway migrations might not complete before tests run

## Recommended Fixes

### Fix 1: Ensure Test Context Loads Properly

Add `@DirtiesContext` to tests that modify shared state:

```java
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class AuthControllerIT extends AbstractPostgresIntegrationTest {
  // ...
}
```

### Fix 2: Add @Sql annotations to reset database state

For tests that need clean database state:

```java
@Sql(scripts = "/cleanup.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
```

### Fix 3: Add proper wait strategies to Testcontainers

Update `AbstractPostgresIntegrationTest.java`:

```java
@Container
static final PostgreSQLContainer<?> POSTGRES =
    new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("docshare_test")
        .withUsername("docshare_test")
        .withPassword("docshare_test")
        .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));
```

### Fix 4: Verify Flyway runs in tests

Ensure test application.yml has:
```yaml
spring:
  flyway:
    enabled: true
    clean-disabled: false  # Allow clean in tests
```

### Fix 5: Add test profiles

Consider separating test configuration by creating specific test profiles for different test types.

## Next Steps

1. Apply the recommended fixes
2. Run integration tests in CI to verify
3. If tests still fail, add debug logging to see actual vs expected responses
4. Consider adding `@BeforeEach` methods to verify infrastructure is ready

## Local Testing Note

Local integration tests are currently failing with:
```
java.lang.IllegalStateException at DockerClientProviderStrategy.java:277
```

This is a Testcontainers + Docker Desktop connectivity issue on macOS. The tests SHOULD work in CI (Linux) where Docker socket access is straightforward.

To test locally on macOS, you may need to:
1. Ensure Docker Desktop is running
2. Set DOCKER_HOST if needed
3. Grant proper permissions to Docker socket
