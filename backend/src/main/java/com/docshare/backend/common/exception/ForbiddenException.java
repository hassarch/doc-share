package com.docshare.backend.common.exception;

/**
 * Thrown when the caller is authenticated but lacks permission for the requested action on a
 * resource they're allowed to know exists (e.g. a Viewer trying to delete a document). Maps to HTTP
 * 403.
 *
 * <p>Per FR-19.9, permission checks that back this exception must happen on every request to a
 * document resource, not just at share time.
 */
public class ForbiddenException extends RuntimeException {

  public ForbiddenException(String message) {
    super(message);
  }
}
