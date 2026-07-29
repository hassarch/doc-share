# Phase 5: Authentication - Implementation Summary

## Overview

Phase 5 implements a complete JWT-based authentication system per FR-1.1 through FR-1.7 of the PRD. This phase fills in the TODO(auth-phase) placeholder left in SecurityConfig during Phase 3.

## What Was Built

### 1. Authentication Endpoints (`/api/v1/auth/*`)

All authentication endpoints are public (no JWT required):

- **POST /api/v1/auth/register** (FR-1.1)
  - Registers new user with email, password, and name
  - Email normalized to lowercase
  - Password bcrypt-hashed (via PasswordEncoder from Phase 3)
  - Returns JWT access token + refresh token
  - Rejects duplicate emails with 409 Conflict

- **POST /api/v1/auth/login** (FR-1.2)
  - Authenticates with email and password
  - Compares password against stored bcrypt hash
  - Returns JWT access token + refresh token
  - Returns 400 for invalid credentials (generic message to prevent enumeration)

- **POST /api/v1/auth/refresh** (FR-1.6)
  - Issues new access token using valid refresh token
  - Reuses same refresh token (no rotation)
  - Returns 400 if refresh token invalid or expired

- **POST /api/v1/auth/logout** (FR-1.3)
  - Revokes refresh token by deleting from Redis
  - Access token remains valid until natural expiration (deliberate design)

- **POST /api/v1/auth/password-reset** (FR-1.4)
  - Initiates password reset flow
  - Always returns 200 to prevent email enumeration
  - Reset token logged server-side (will be emailed once Notification Service exists)
  - Token stored in Redis with 1-hour TTL

### 2. JWT Implementation

**JwtService** (`auth.service.JwtService`):
- Generates signed JWTs using JJWT library
- Access token contains: userId (as subject), email (as claim)
- Access token TTL: 15 minutes (configurable via `docshare.jwt.access-token-ttl-minutes`)
- Secret key: loaded from config (`docshare.jwt.secret`)
- Validates signature and expiration
- Returns null for invalid/expired tokens (caller handles as unauthorized)

**Access Tokens**:
- Short-lived (15 min) signed JWTs
- Stateless validation - no database lookup required
- UUID user ID stored as subject
- Email stored as custom claim
- HMAC-SHA256 signature

**Refresh Tokens**:
- Long-lived (7 days) opaque random strings
- 32 bytes of secure random, Base64-URL encoded
- Stored in Redis with TTL: `refresh_token:<token>` → userId
- Revocable via Redis DEL (logout)
- Used only to issue new access tokens, never for API authorization

### 3. Authentication Filter

**JwtAuthenticationFilter** (`auth.service.JwtAuthenticationFilter`):
- Extends `OncePerRequestFilter` (runs once per request)
- Extracts JWT from `Authorization: Bearer <token>` header
- Validates token using `JwtService`
- Populates Spring Security's `SecurityContext` with authenticated principal
- Principal is UUID (user ID) - controllers can cast to UUID
- Grants single authority: `ROLE_USER`
- Public endpoints (defined in SecurityConfig) still pass through filter but don't require authentication

### 4. SecurityConfig Update

Filled in the TODO from Phase 3:
- Added `JwtAuthenticationFilter` to security filter chain
- Filter runs before `UsernamePasswordAuthenticationFilter`
- JWT validation now enforces FR-1.7: all endpoints except `/api/v1/auth/*` require valid JWT
- Public paths unchanged: `/api/v1/auth/**`, `/actuator/health/**`, `/actuator/info`

### 5. Data Transfer Objects

Created DTOs in `auth.dto` package:
- `RegisterRequest` - email, password (min 8 chars), name
- `LoginRequest` - email, password
- `RefreshRequest` - refresh token
- `PasswordResetRequest` - email
- `AuthResponse` - access token, refresh token

All DTOs use Jakarta Bean Validation annotations (`@NotBlank`, `@Email`, `@Size`).

### 6. Authentication Service

**AuthenticationService** (`auth.service.AuthenticationService`):
- Handles core authentication logic
- Does NOT use Spring Security's `AuthenticationManager` - deliberate choice explained in code comments
- Manually compares password hashes using `PasswordEncoder`
- Issues JWT pairs on successful authentication
- Stores/retrieves refresh tokens in Redis
- Generates cryptographically secure random tokens using `SecureRandom`

Key methods:
- `register()` - Creates user, returns tokens
- `login()` - Validates credentials, returns tokens
- `refresh()` - Issues new access token from refresh token
- `logout()` - Deletes refresh token from Redis
- `initiatePasswordReset()` - Stores reset token in Redis, logs it (TODO: email it)

### 7. Test Infrastructure Improvements

**Problem Fixed:**
Phase 3's BackendApplicationTests used `application-test.yml` to exclude DataSource/JPA/Redis autoconfiguration, assuming a "plain context load" didn't need infrastructure. This broke when Phase 4 added real `@Repository` interfaces (which need a DataSource) and became untenable for Phase 5 (which needs Redis).

**Solution:**
- Retired exclusion-based smoke test
- Created `AbstractIntegrationTest` base class
- Provides Testcontainers-managed Postgres + Redis
- Containers marked reusable (`testcontainers.reuse.enable=true`)
- All tests now run against real infrastructure

**Tests Added:**
- `AbstractIntegrationTest` - Shared Testcontainers base
- `AuthControllerIT` - Integration test for full auth flow (8 test scenarios)
- `JwtServiceTest` - Unit test for JWT generation/validation

**Tests Updated:**
- `BackendApplicationTests` - Now extends AbstractIntegrationTest
- `UserRepositoryIT` - Now extends AbstractIntegrationTest

### 8. Documentation

- **backend/TEST_AUTH.md** - Manual testing guide with curl commands for all auth endpoints
- **backend/README.md** - Updated with auth endpoint documentation
- **README.md** (root) - Updated status to Phase 5 complete

## Design Decisions & Rationale

### Why Two Token Types?

**Access Token (JWT):**
- Purpose: Authorization for API requests
- Benefit: Stateless validation scales horizontally - any instance can validate with just the shared secret
- Tradeoff: Cannot be revoked once issued (valid until expiration)

**Refresh Token (Opaque, Redis-backed):**
- Purpose: Issue new access tokens without re-entering credentials
- Benefit: Revocable (logout works by deleting from Redis)
- Stored centrally: No crypto involved, just a Redis lookup

**Why not just use long-lived JWTs?** Because you can't revoke them. Logout wouldn't work.

**Why not blacklist JWTs?** Because that defeats the entire stateless scaling benefit - you'd need to check a blacklist on every request, which requires a central datastore lookup anyway.

**The compromise:** Short-lived access tokens (15 min) that are stateless, paired with long-lived refresh tokens (7 days) that are revocable. Logout deletes the refresh token; the old access token expires naturally within 15 minutes - an acceptable, documented window.

### Why Not Use Spring Security's AuthenticationManager?

Spring Security's `AuthenticationManager` is designed for filter-based authentication that establishes a session or immediately-usable principal (e.g., form login). 

Our login endpoint is different:
- It's a REST endpoint that returns tokens as JSON
- It doesn't establish a session (we're stateless)
- It doesn't need to populate SecurityContext (that happens later via JwtAuthenticationFilter)

Manually comparing the password hash and issuing tokens is simpler and more explicit than configuring an `AuthenticationProvider` to do the same thing.

The JWT filter *does* integrate with Spring Security by populating `SecurityContext` - that part follows the standard pattern.

### Why Generic Response for Password Reset?

Always returning "If that email is registered, a password reset link has been sent" prevents account enumeration attacks. If the response differed based on whether the email exists, an attacker could use this endpoint to discover registered accounts.

### Why Log Reset Tokens Instead of Emailing?

The Notification Service (which handles email sending) will be implemented in a future phase. For Phase 5, we're implementing FR-1.4 (password reset token issuance) but not FR-1.5 (using the token to complete reset). The token is logged server-side for manual testing.

## Configuration

New properties in `application.yml`:

```yaml
docshare:
  jwt:
    secret: ${JWT_SECRET:change-me-in-every-environment-this-is-a-local-dev-only-default}
    access-token-ttl-minutes: 15
    refresh-token-ttl-days: 7
```

**Important:** The default secret is only safe for local development. Production deployments MUST set a unique `JWT_SECRET` environment variable.

## Redis Key Patterns

The system uses these Redis key patterns:

- `refresh_token:<token>` → userId (TTL: 7 days)
- `password_reset:<token>` → userId (TTL: 1 hour)

## Testing

### Automated Tests

Run with:
```bash
./gradlew test -x spotlessJava
```

**Note:** Tests require Docker to be running for Testcontainers. If Docker is unavailable, tests will fail with `ContainerFetchException`.

Test coverage:
- Context loading with real infrastructure (BackendApplicationTests)
- Full authentication flow (AuthControllerIT):
  - Registration success/duplicate rejection
  - Login success/invalid credentials
  - Token refresh
  - Logout and token revocation
  - Password reset flow
- JWT generation/validation (JwtServiceTest)

### Manual Testing

See `backend/TEST_AUTH.md` for detailed curl-based testing scenarios.

Quick smoke test:
```bash
# Start infrastructure
cd infra && docker-compose up -d

# Start backend
cd backend && ./gradlew bootRun

# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@docshare.local","password":"password123","name":"Test User"}'

# Should return 201 with access/refresh tokens
```

## Known Limitations & Future Work

1. **Access token remains valid after logout** - Deliberate design. The 15-minute window is acceptable for this system. If stricter revocation is needed, options include:
   - Maintain a JWT blacklist in Redis (but this sacrifices stateless validation)
   - Reduce access token TTL (but increases refresh traffic)
   - Implement short-polling or websocket notification for immediate logout

2. **Password reset incomplete** - FR-1.4 (token issuance) is done; FR-1.5 (using token to set new password) is not. Will be added when prioritized.

3. **No email notifications** - Reset tokens are logged, not emailed. Notification Service will be implemented in a future phase.

4. **No rate limiting** - Authentication endpoints are public and should be rate-limited to prevent brute-force attacks. This will be added in the Security hardening phase.

5. **No refresh token rotation** - Some implementations rotate refresh tokens on each use. We chose not to for simplicity. Can be added if needed.

6. **Single role only** - All authenticated users get `ROLE_USER`. Role-based access control (admin, etc.) can be added if the PRD requires it.

## Files Changed/Added

**New files:**
- `auth/controller/AuthController.java`
- `auth/dto/` (5 files)
- `auth/service/AuthenticationService.java`
- `auth/service/JwtAuthenticationFilter.java`
- `auth/service/JwtService.java`
- `test/.../AbstractIntegrationTest.java`
- `test/.../auth/controller/AuthControllerIT.java`
- `test/.../auth/service/JwtServiceTest.java`
- `backend/TEST_AUTH.md`
- `PHASE5_SUMMARY.md`

**Modified files:**
- `config/SecurityConfig.java` - Added JWT filter
- `build.gradle.kts` - Added testcontainers dependency
- `BackendApplicationTests.java` - Now extends AbstractIntegrationTest
- `users/repository/UserRepositoryIT.java` - Now extends AbstractIntegrationTest
- `backend/README.md` - Added auth endpoint docs
- `README.md` (root) - Updated status

## Git History

```
407f6d2 Update main README with Phase 5 completion status
b22207b Add manual authentication testing guide
390c970 Phase 5: Implement JWT-based authentication system
```

## Next Steps

Phase 5 is complete and ready for merge to `feature/database`, then to `main`.

Suggested next phases:
- **Phase 6: Document Storage** - MinIO integration, file upload/download, chunking
- **Phase 7: Folder Management** - Folder hierarchy, move/rename operations
- **Phase 8: Sharing** - Share links, permissions, access control
- **Phase 9: Synchronization** - Kafka-based event streaming, conflict resolution

## Verification Checklist

Before merging:
- [x] All authentication endpoints implemented (FR-1.1-1.4, 1.6-1.7)
- [x] JWT filter wired into SecurityConfig
- [x] Protected endpoints require valid JWT
- [x] Public endpoints remain accessible
- [x] Refresh tokens stored in Redis
- [x] Logout revokes refresh token
- [x] Password reset token issued and logged
- [x] Code compiles (`./gradlew compileJava` succeeds)
- [x] Tests created (integration + unit)
- [x] Documentation updated
- [x] Manual testing guide provided
- [x] Design decisions documented
