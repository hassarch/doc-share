# Integration Test Debugging Strategy

## Current Status

We've been systematically fixing integration test failures in CI. Here's what we've done:

## Fixes Applied (in order)

### Round 1: Initial Issues
1. ✅ **Security**: Removed hardcoded secrets from health-check.sh
2. ✅ **MinIO Init**: Added missing storage configuration for tests
3. ✅ **Container Wait**: Added proper wait strategies for all containers

### Round 2: Current Issues (addressing now)
The tests are still failing, but with different symptoms:

**Symptom 1**: HTTP endpoint assertions failing
```
AuthControllerIT > registerThenLoginThenRefreshThenLogout() FAILED
    java.lang.AssertionError at line 41
```
- Register endpoint not returning 201 Created
- All Auth/Document controller tests failing similarly

**Symptom 2**: Database connection failures mid-test-run
```
NotificationServiceIT > unreadCount_countsUnreadOnly() FAILED
    CannotCreateTransactionException
    Caused by: JDBCConnectionException  
    Caused by: ConnectException
```
- Some tests can't connect to Postgres
- Suggests containers or connections are being closed

**Root Cause Hypothesis**: Tests may be running in parallel, causing resource conflicts, or the shared Spring context is in a bad state.

## Latest Fixes (Commit 49fe5f0)

### 1. Sequential Test Execution
```kotlin
maxParallelForks = 1  // Run one test class at a time
```
**Why**: Prevents multiple tests from competing for container resources

### 2. Disable Container Reuse
```java
.withReuse(false)  // Each test run gets fresh containers
```
**Why**: Ensures clean state, no leftover data or connections

### 3. Proper Web Environment
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
```
**Why**: Ensures each test gets its own port, preventing conflicts

### 4. Extended Timeouts
```yaml
spring.datasource.hikari:
  connection-timeout: 60000
  max-lifetime: 120000
  idle-timeout: 60000
  leak-detection-threshold: 60000
```
**Why**: CI environments are slower than local; prevents premature connection closure

### 5. Increased Connection Pool
```yaml
spring.datasource.hikari:
  maximum-pool-size: 10
```
**Why**: Allows more concurrent connections if tests need them

## What To Watch For

### If Tests Still Fail:

#### Scenario A: All tests fail with "context load" errors
**Meaning**: Spring Boot can't start up
**Next Steps**:
- Check application logs in CI artifacts
- Look for bean creation failures
- Verify all required properties are set

#### Scenario B: First few tests pass, later tests fail
**Meaning**: Resource leak or timeout issue
**Next Steps**:
- Add `@DirtiesContext` to force context reload
- Investigate connection pool exhaustion
- Check for unclosed resources

#### Scenario C: Random tests fail inconsistently
**Meaning**: Race condition or timing issue
**Next Steps**:
- Add `@TestMethodOrder` for deterministic execution
- Increase wait times in tests
- Check for async operations not completing

#### Scenario D: Specific test always fails
**Meaning**: Test-specific issue
**Next Steps**:
- Run that test in isolation locally
- Check test assumptions about data state
- Verify mocked services are working

## Debugging Commands

### Run tests locally with full output:
```bash
cd backend
./gradlew integrationTest --info
```

### Run single test class:
```bash
./gradlew integrationTest --tests "AuthControllerIT"
```

### Run single test method:
```bash
./gradlew integrationTest --tests "AuthControllerIT.registerThenLoginThenRefreshThenLogout"
```

### Check container logs (if tests fail):
```bash
docker ps -a  # See all containers
docker logs <container-id>  # Check specific container logs
```

## Expected CI Behavior After Latest Fix

1. ✅ Containers start successfully (we've seen this working)
2. ✅ Spring context loads (BackendApplicationTests passed before)
3. ⏳ **NEW**: Tests run sequentially without interference
4. ⏳ **NEW**: Connections stay alive throughout test run
5. ⏳ **NEW**: All 16 tests pass

## Timeline of Issues

| Commit | Issue | Status |
|--------|-------|--------|
| f7c50e3 | Initial deployment fixes | ✅ Passed |
| f9ce8b9 | Hardcoded secrets | ✅ Fixed |
| 517e4bc | Missing test config | ✅ Fixed (MinIO now initializes) |
| ab54012 | Storage properties | ✅ Fixed |
| 5c5dd11 | Documentation | N/A |
| 49fe5f0 | Sequential execution | ⏳ Testing now |

## If All Else Fails

### Option 1: Add Explicit Logging
Add this to failing tests to see actual responses:
```java
MvcResult result = mockMvc.perform(...)
    .andReturn();
System.out.println("Status: " + result.getResponse().getStatus());
System.out.println("Body: " + result.getResponse().getContentAsString());
```

### Option 2: Reduce Test Scope
Temporarily disable flaky tests to get CI green:
```java
@Disabled("Temporarily disabled - investigating flakiness")
@Test
void problematicTest() { ... }
```

### Option 3: Use Test Retry
Add test retry for flaky tests:
```kotlin
// In build.gradle.kts
tasks.withType<Test> {
    retry {
        maxRetries.set(2)
        maxFailures.set(5)
    }
}
```

### Option 4: Split Test Suites
Run different test groups separately:
- Unit tests
- Controller integration tests
- Service integration tests
- Repository integration tests

## Success Criteria

Tests are considered fixed when:
- ✅ All 16 integration tests pass consistently
- ✅ No connection errors mid-run
- ✅ No assertion failures on HTTP endpoints
- ✅ Total execution time < 15 minutes
- ✅ Tests pass 3 times in a row in CI

## Current Commit

```
49fe5f0 - Configure integration tests for sequential execution and better reliability
```

This commit should fix the race conditions and connection issues. CI is running now - check GitHub Actions for results.
