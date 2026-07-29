# Authentication Testing Guide

Manual test scenarios for the JWT-based authentication system (Phase 5).

## Prerequisites

1. Start the infrastructure:
   ```bash
   cd ../infra
   docker-compose up -d
   ```

2. Start the backend:
   ```bash
   ./gradlew bootRun
   ```

## Test Scenarios

### 1. Register a new user

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "ada@docshare.local",
    "password": "password123",
    "name": "Ada Lovelace"
  }'
```

**Expected response (201 Created):**
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "aBcDeFg..."
}
```

Save both tokens for the next steps.

### 2. Try to register duplicate email

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "ada@docshare.local",
    "password": "password123",
    "name": "Ada Lovelace"
  }'
```

**Expected response (409 Conflict):**
```json
{
  "timestamp": "2024-...",
  "traceId": "...",
  "errors": [
    {
      "field": null,
      "message": "Email already registered"
    }
  ]
}
```

### 3. Login with correct credentials

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "ada@docshare.local",
    "password": "password123"
  }'
```

**Expected response (200 OK):**
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "aBcDeFg..."
}
```

### 4. Login with incorrect password

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "ada@docshare.local",
    "password": "wrongpassword"
  }'
```

**Expected response (400 Bad Request):**
```json
{
  "timestamp": "2024-...",
  "traceId": "...",
  "errors": [
    {
      "field": null,
      "message": "Invalid email or password"
    }
  ]
}
```

### 5. Access protected endpoint without token

```bash
curl -X GET http://localhost:8080/api/v1/documents
```

**Expected response (401 Unauthorized):**
```json
{
  "timestamp": "2024-...",
  "status": 401,
  "error": "Unauthorized",
  "path": "/api/v1/documents"
}
```

### 6. Access protected endpoint with valid token

Replace `<ACCESS_TOKEN>` with the token from step 1 or 3:

```bash
curl -X GET http://localhost:8080/api/v1/documents \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

**Expected response:**
- If documents endpoint exists: appropriate response
- If not yet implemented: 404 (but authenticated successfully)

### 7. Refresh the access token

Replace `<REFRESH_TOKEN>` with the refresh token from step 1 or 3:

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<REFRESH_TOKEN>"
  }'
```

**Expected response (200 OK):**
```json
{
  "accessToken": "eyJhbGc...",  // New access token
  "refreshToken": "<REFRESH_TOKEN>"  // Same refresh token
}
```

### 8. Logout (revoke refresh token)

Replace `<REFRESH_TOKEN>` with the refresh token:

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<REFRESH_TOKEN>"
  }'
```

**Expected response (200 OK):**
```json
{
  "message": "Logged out successfully"
}
```

### 9. Try to refresh with revoked token

Use the same refresh token from step 8:

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<REFRESH_TOKEN>"
  }'
```

**Expected response (400 Bad Request):**
```json
{
  "timestamp": "2024-...",
  "traceId": "...",
  "errors": [
    {
      "field": null,
      "message": "Invalid or expired refresh token"
    }
  ]
}
```

### 10. Initiate password reset

```bash
curl -X POST http://localhost:8080/api/v1/auth/password-reset \
  -H "Content-Type: application/json" \
  -d '{
    "email": "ada@docshare.local"
  }'
```

**Expected response (200 OK):**
```json
{
  "message": "If that email is registered, a password reset link has been sent"
}
```

**Note:** Check server logs for the reset token (it's logged, not emailed yet):
```
Password reset token (LOG ONLY — will be emailed once Notification Service is ready): email=ada@docshare.local, token=...
```

### 11. Password reset for non-existent email

```bash
curl -X POST http://localhost:8080/api/v1/auth/password-reset \
  -H "Content-Type: application/json" \
  -d '{
    "email": "nobody@docshare.local"
  }'
```

**Expected response (200 OK):**
```json
{
  "message": "If that email is registered, a password reset link has been sent"
}
```

**Note:** Same response as step 10 — this prevents email enumeration.

## Validation checklist

- [ ] Registration creates user and returns tokens
- [ ] Duplicate email registration is rejected
- [ ] Login with correct credentials succeeds
- [ ] Login with incorrect credentials fails
- [ ] Protected endpoints reject requests without token
- [ ] Protected endpoints accept requests with valid token
- [ ] Refresh token issues new access token
- [ ] Logout revokes refresh token
- [ ] Revoked refresh token cannot be used
- [ ] Password reset returns generic response for all emails
- [ ] Password reset token is logged for registered users

## Known limitations (Phase 5)

1. **Access token remains valid after logout** — This is deliberate. Once issued, a JWT is valid until expiration (15 min). Logout revokes the refresh token only, preventing new access tokens from being issued. The old access token expires naturally.

2. **Password reset token is logged, not emailed** — The Notification Service (which handles email sending) will be implemented in a future phase. For now, reset tokens are logged server-side.

3. **No password reset completion endpoint** — The reset token issuance is implemented (FR-1.4), but the "use reset token to set new password" endpoint (FR-1.5) will be added once the full password reset flow is prioritized.
