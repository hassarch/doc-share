package com.docshare.backend.auth.security;

import com.docshare.backend.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fills the slot marked {@code TODO(auth-phase)} in {@code SecurityConfig} since Phase 3: reads the
 * {@code Authorization: Bearer <token>} header, validates it via {@link JwtService}, and — if valid
 * — populates the {@code SecurityContext} with an authenticated principal (the user's ID) so
 * downstream controllers can rely on {@code SecurityContextHolder.getContext().getAuthentication()}
 * rather than re-parsing the token themselves.
 *
 * <p>If the header is missing or invalid, this filter does nothing and lets the request continue
 * unauthenticated — {@code SecurityConfig}'s {@code anyRequest().authenticated()} is what actually
 * rejects it with 401 for protected paths. This filter never throws for a missing/bad token; it
 * only ever <em>adds</em> an authentication when one is valid.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTH_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(AUTH_HEADER);

    if (header != null && header.startsWith(BEARER_PREFIX)) {
      String token = header.substring(BEARER_PREFIX.length());
      try {
        UUID userId = jwtService.validateAndGetUserId(token);
        var authentication =
            new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (JwtService.JwtValidationException e) {
        // Deliberately swallowed: an invalid/expired token just means the
        // request proceeds unauthenticated, and SecurityConfig's
        // authorization rule rejects it with 401 if the path requires
        // auth. Throwing here would leak "your token specifically failed
        // validation" as a distinct error path from "you sent no token at
        // all," which isn't a distinction callers need.
      }
    }

    filterChain.doFilter(request, response);
  }
}
