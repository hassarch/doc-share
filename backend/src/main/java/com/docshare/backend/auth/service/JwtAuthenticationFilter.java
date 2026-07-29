package com.docshare.backend.auth.service;

import io.jsonwebtoken.Claims;
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
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Extracts and validates JWTs from the {@code Authorization: Bearer <token>} header, populating
 * Spring Security's {@code SecurityContext} with an authenticated principal if the token is valid.
 * This is what makes FR-1.7 ("all endpoints except {@code /auth/*} require a valid JWT") actually
 * work — without this filter, even though {@code SecurityConfig} says "anyRequest().authenticated()",
 * nothing in the chain knows how to produce an authenticated principal from a JWT.
 *
 * <p>Runs on every request (once per request, hence {@code OncePerRequestFilter}). Public paths
 * like {@code /auth/*} still pass through this filter, but since {@code SecurityConfig} marked
 * them {@code permitAll()}, Spring Security won't reject the request even if no token is present.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);
      Claims claims = jwtService.validateToken(token);

      if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UUID userId = jwtService.getUserIdFromClaims(claims);
        String email = jwtService.getEmailFromClaims(claims);

        // No roles/permissions system in this phase — just a single "USER" authority
        // for now. Future phases (e.g., admin features, if added) can expand this.
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userId, // principal is the user ID — controllers can cast this to UUID
                null, // credentials (password) are irrelevant after authentication
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    filterChain.doFilter(request, response);
  }
}
