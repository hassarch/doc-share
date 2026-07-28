package com.docshare.backend.common.dto;

/**
 * Top-level wrapper so every error response body is shaped {@code {"error": {...}}}, never a bare
 * error object at the root — this leaves room to add sibling top-level keys later (e.g. {@code
 * "warnings"}) without a breaking change to existing clients.
 */
public record ErrorEnvelope(ErrorResponse error) {

  public static ErrorEnvelope of(ErrorResponse error) {
    return new ErrorEnvelope(error);
  }
}
