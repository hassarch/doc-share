package com.docshare.backend.common.exception;

import com.docshare.backend.common.dto.ErrorEnvelope;
import com.docshare.backend.common.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Single place where every exception thrown by a controller (in any module) becomes a consistent
 * {@link ErrorEnvelope} JSON body, per ADR-0001 #7.
 *
 * <p>Adding a new domain exception type: add the exception class next to the others in this
 * package, then add one {@code @ExceptionHandler} method here. Do not catch exceptions manually in
 * individual controllers — that's exactly the per-module inconsistency this class exists to
 * prevent.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorEnvelope> handleNotFound(NotFoundException ex) {
    return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ErrorEnvelope> handleForbidden(ForbiddenException ex) {
    return respond(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage());
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorEnvelope> handleValidation(ValidationException ex) {
    return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", ex.getMessage());
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorEnvelope> handleConflict(ConflictException ex) {
    return respond(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
  }

  /** Bean Validation failures on {@code @Valid} request bodies. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorEnvelope> handleBeanValidation(MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .orElse("Validation failed");
    return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message);
  }

  /**
   * Catch-all for anything unexpected. Deliberately does NOT leak the raw exception message to the
   * client — only a generic message plus the traceId, so the caller can report it and an operator
   * can find the full stack trace in the logs by that traceId. Never remove this fallback: without
   * it, an unhandled exception would return Spring's default HTML error page instead of our JSON
   * envelope.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorEnvelope> handleUnexpected(Exception ex) {
    String traceId = currentTraceId();
    log.error("Unhandled exception [traceId={}]", traceId, ex);
    ErrorResponse error =
        ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred.", traceId);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorEnvelope.of(error));
  }

  private ResponseEntity<ErrorEnvelope> respond(HttpStatus status, String code, String message) {
    String traceId = currentTraceId();
    ErrorResponse error = ErrorResponse.of(code, message, traceId);
    return ResponseEntity.status(status).body(ErrorEnvelope.of(error));
  }

  private String currentTraceId() {
    String traceId = MDC.get("traceId");
    return traceId != null ? traceId : "unknown";
  }
}
