package com.docshare.backend.common.dto;

import java.time.Instant;

/**
 * The single JSON shape every error response uses, system-wide, per ADR-0001 #7 ("consistent error
 * formatting").
 *
 * <p>Serializes as:
 *
 * <pre>{@code
 * {
 *   "error": {
 *     "code": "NOT_FOUND",
 *     "message": "Document 3f2... not found",
 *     "traceId": "a1b2c3d4",
 *     "timestamp": "2026-07-28T10:00:00Z"
 *   }
 * }
 * }</pre>
 *
 * @param code a stable, machine-readable error code (e.g. {@code NOT_FOUND}, {@code
 *     VALIDATION_FAILED}) — frontend code should branch on this, never on the human-readable
 *     message
 * @param message a human-readable explanation, safe to show to the end user
 * @param traceId the correlation ID for this request, from {@code CorrelationIdFilter} — lets you
 *     find the matching server-side logs
 * @param timestamp when the error was produced
 */
public record ErrorResponse(String code, String message, String traceId, Instant timestamp) {

  public static ErrorResponse of(String code, String message, String traceId) {
    return new ErrorResponse(code, message, traceId, Instant.now());
  }
}
