package com.docshare.backend.common.exception;

/**
 * Thrown when a requested resource doesn't exist, or (deliberately) when it exists but the caller
 * isn't allowed to know that — see {@link ForbiddenException} for when leaking existence is
 * acceptable but access still needs denying. Maps to HTTP 404.
 */
public class NotFoundException extends RuntimeException {

  public NotFoundException(String message) {
    super(message);
  }
}
