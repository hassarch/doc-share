package com.docshare.backend.common.util;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reads the authenticated user's ID out of Spring Security's {@code
 * SecurityContext}, where {@code auth.security.JwtAuthenticationFilter}
 * placed it as the principal. Kept in {@code common.util} rather than
 * {@code auth} since it's a thin, generic accessor with no auth business
 * logic — any controller in any module needs this, and none of them
 * should have to import from {@code auth} just to ask "who is making this
 * request."
 */
public final class CurrentUser {

  private CurrentUser() {}

  /**
   * Returns the authenticated user's ID. Throws {@link IllegalStateException}
   * if called on an unauthenticated thread — which should be unreachable
   * for any controller behind {@code SecurityConfig}'s {@code
   * anyRequest().authenticated()} rule; if you see this exception, the
   * endpoint is probably missing from that filter chain's protection as
   * expected, not that the user is somehow "invalid."
   */
  public static UUID id() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof UUID userId)) {
      throw new IllegalStateException(
          "No authenticated user in SecurityContext — this endpoint should be behind"
              + " authentication");
    }
    return userId;
  }
}
