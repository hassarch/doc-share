package com.docshare.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Initiates a password reset (FR-1.4). Always returns the same generic success response whether or
 * not the email exists, to prevent account enumeration.
 *
 * <p>The reset token will eventually be emailed (once the Notification Service is built), but for
 * now it's only logged server-side for manual retrieval in local testing.
 */
public record PasswordResetRequest(
    @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email) {}
