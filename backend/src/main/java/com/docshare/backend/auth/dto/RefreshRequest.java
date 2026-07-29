package com.docshare.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Requests a new access token using a still-valid refresh token (FR-1.6). */
public record RefreshRequest(@NotBlank(message = "Refresh token is required") String refreshToken) {}
