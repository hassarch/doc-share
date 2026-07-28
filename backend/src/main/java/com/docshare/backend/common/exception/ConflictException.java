package com.docshare.backend.common.exception;

/**
 * Thrown when a request conflicts with the current state of a resource — most notably the
 * optimistic-locking conflict described in FR-18.3: a second writer's commit against a stale
 * document version. Maps to HTTP 409.
 *
 * <p>Callers receiving this should re-fetch the current state and retry; this exception (and the
 * 409 it produces) deliberately never triggers a silent overwrite.
 */
public class ConflictException extends RuntimeException {

  public ConflictException(String message) {
    super(message);
  }
}
