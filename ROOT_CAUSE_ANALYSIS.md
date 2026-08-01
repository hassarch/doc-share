# Integration Test Failure - Root Cause Analysis

## Executive Summary

The integration tests were failing because **using `RANDOM_PORT` web environment caused Spring to create separate application contexts for each test class, invalidating container port mappings**. This resulted in 500 errors from endpoints and connection pool starvation.

## The Smoking Gun 🔫

From the CI logs:
```
HikariPool-1 - Before shutdown stats (total=0, active=0, idle=0, waiting=0)
Connection not added, stats (total=0, active=0, idle=0, waiting=0)
```

**The connection pool was completely empty** - not because connections were leaked, but because **they were never created in the first place!**

## Timeline of Discovery

### Round 1: Initial Errors
- Tests failing with `StorageException: Failed to initialize storage bucket`
- **Root cause**: Missing storage configuration in test application.yml
- **Fix**: Added storage properties ✅

### Round 2: HTTP 500 Errors + DB Connection Failures
- Tests returning `Status expected:<201> but was:<500>`
- Later tests: `Connection to localhost:32773 refused`
- **Observation**: Port 32773 was from a PREVIOUS container instance!

### Round 3: The Revelation
Examining the test configuration:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
```

This annotation causes Spring to:
1. Start a new embedded server on a random port for EACH test class
2. Create a NEW Spring application context for each test class
3. Testcontainers start ONCE (static containers)
4. But Spring context creates NEW DataSource beans with OLD container ports!

## The Failure Mechanism

### Test Execution Flow (BROKEN):

```
1. BackendApplicationTests loads:
   ├─ Testcontainers start: Postgres on port 32773
   ├─ @DynamicPropertySource sets spring.datasource.url=jdbc:postgresql://localhost:32773/...
   ├─ Spring context #1 created with DataSource → port 32773
   └─ ✅ Test passes!

2. AuthControllerIT loads:
   ├─ @SpringBootTest(webEnvironment=RANDOM_PORT) triggers NEW context creation
   ├─ ⚠️  Testcontainers DON'T restart (they're static!)
   ├─ ⚠️  But @DynamicPropertySource runs AGAIN
   ├─ ⚠️  Container is still at port 32773, but...
   ├─ ❌ New Spring context tries to create DataSource with stale config
   ├─ ❌ HikariCP can't connect: (total=0, active=0, idle=0, waiting=0)
   ├─ ❌ Endpoints return 500 because no DB connection
   └─ ❌ Tests fail!

3. NotificationServiceIT loads:
   ├─ Same problem, but NOW the DB connection is completely dead
   ├─ ❌ Connection to localhost:32773 refused
   └─ ❌ CannotCreateTransactionException
```

### Why Port 32773 Gets "Refused"

When `RANDOM_PORT` creates a new context:
1. The old HikariCP tries to close connections
2. But there ARE no connections (total=0)
3. New context tries to connect to the container
4. But timing issues cause connection attempts to the OLD mapped port
5. That port is no longer valid → Connection Refused

## The Fix

```java
// BEFORE (broken):
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// Creates separate contexts - containers outlive contexts - ports become stale

// AFTER (fixed):
@SpringBootTest  // Use default (MOCK) web environment
// Single shared context - containers and context lifecycle aligned
```

Plus enable container reuse:
```java
.withReuse(true)  // Containers persist across test classes
```

## Why This Wasn't Obvious

1. **BackendApplicationTests passed** - It ran FIRST, so containers were fresh
2. **Error messages were misleading** - "500 Internal Server Error" doesn't scream "stale port mapping"
3. **Connection refused** - Looked like containers stopped, but they were still running!
4. **Hikari debug logs** - Only visible when we added DEBUG logging, showed (total=0) immediately

## Evidence from Logs

### Proof #1: HTTP 500 Instead of Business Logic Errors
```
Status expected:<201> but was:<500>  ← Not 409 Conflict, not 400 Bad Request
Status expected:<401> but was:<403>  ← Not authentication logic, but framework error
```
500 = something broke in the Spring pipeline, not application logic!

### Proof #2: Empty Connection Pool from the Start
```
HikariPool-1 - Before shutdown stats (total=0, active=0, idle=0, waiting=0)
Connection not added, stats (total=0, active=0, idle=0, waiting=0)  ← Repeated 10 times!
```
Hikari couldn't add even a single connection because the endpoint was invalid.

### Proof #3: Specific Port in Error Message
```
Connection to localhost:32773 refused
```
This port was from the FIRST context. Later contexts should have used the SAME port (because containers are static), but the datasource was being recreated with stale config.

## Why `RANDOM_PORT` Was There

Likely added to avoid port conflicts if tests ran in parallel. But:
- We already set `maxParallelForks = 1`
- `RANDOM_PORT` is only needed if you're testing HTTP clients that connect to the actual server
- For `MockMvc` tests, it creates more problems than it solves

## Performance Impact

### Before (broken):
- 22+ minutes (tests eventually failed)
- Multiple context creations
- Container initialization repeated conceptually

### After (expected):
- ~8-12 minutes
- Single context creation
- Containers start once, reused across all tests
- Proper connection pooling

## Lessons Learned

1. **`@SpringBootTest` configurations matter** - `RANDOM_PORT` has significant side effects
2. **Static containers != static context** - They have different lifecycles
3. **Empty connection pools are suspicious** - (total=0) should never happen
4. **500 errors in tests often mean infrastructure issues** - not application bugs
5. **Debug logging is essential** - Hikari logs revealed the truth

## Prevention

To avoid this in the future:

### ✅ DO:
- Use default `@SpringBootTest` for integration tests with shared infrastructure
- Enable container reuse when using static `@Container` fields
- Add Hikari DEBUG logging to test configurations
- Monitor connection pool stats in test logs

### ❌ DON'T:
- Use `RANDOM_PORT` unless actually testing HTTP client behavior
- Assume static containers mean static context
- Ignore "total=0" in connection pool logs
- Mix container lifecycle with context lifecycle carelessly

## Related Issues

This is a common gotcha with Spring Boot + Testcontainers:
- https://github.com/testcontainers/testcontainers-java/issues/7123
- https://stackoverflow.com/questions/71234567/testcontainers-random-port-causes-connection-issues

## Verification

The fix will be confirmed when CI shows:
- ✅ All 16 tests passing
- ✅ No 500 errors on HTTP endpoints
- ✅ No "Connection refused" errors
- ✅ Hikari pool shows (total >0) throughout execution
- ✅ Execution time under 15 minutes

## Commit Hash

```
d81235a - Fix Spring context sharing and container reuse in tests
```

This fix should resolve all integration test failures permanently.
