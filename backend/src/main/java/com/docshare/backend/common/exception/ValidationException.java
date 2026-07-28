package com.docshare.backend.common.exception;

/**
 * Thrown for request-shape or business-rule validation failures that aren't already covered by Bean
 * Validation annotations (e.g. "file exceeds the configured size limit", "chunk checksum
 * mismatch"). Maps to HTTP 400.
 *
 * <p>For simple field-level validation (missing/malformed fields), prefer {@code
 * jakarta.validation} annotations on the request DTO — {@link GlobalExceptionHandler} already maps
 * {@code MethodArgumentNotValidException} to the same error shape. Reach for this exception only
 * for validation logic that can't be expressed as an annotation.
 */
public class ValidationException extends RuntimeException {

  public ValidationException(String message) {
    super(message);
  }
}
