package com.docshare.backend.auth.dto;

/**
 * Returned from login and refresh endpoints. Access token is short-lived (15 min) and signed —
 * validated entirely by JWT signature, no database lookup. Refresh token is long-lived (7 days),
 * opaque, stored in Redis — used only to issue new access tokens, never for API authorization.
 */
public record AuthResponse(String accessToken, String refreshToken) {}
