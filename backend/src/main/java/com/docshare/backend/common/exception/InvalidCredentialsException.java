package com.docshare.backend.common.exception;

/**
 * Thrown for a failed login attempt (unknown email or wrong password) or an invalid/expired refresh
 * token. Maps to HTTP 401.
 *
 * <p>Deliberately carries the same message regardless of whether the email exists or the password
 * was wrong — distinguishing those in the response would let an attacker enumerate registered
 * emails via the login endpoint.
 */
public class InvalidCredentialsException extends RuntimeException {

  public InvalidCredentialsException(String message) {
    super(message);
  }
}
