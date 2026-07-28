package com.docshare.backend.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * The shape of request authorization for this application — which paths are public, which require
 * authentication, and the session/CORS/CSRF posture.
 *
 * <p><strong>What this class deliberately does NOT do yet:</strong> validate JWTs. Per FR-1.7 ("all
 * endpoints except {@code /auth/*} require a valid JWT"), a {@code JwtAuthenticationFilter} needs
 * to sit in this chain — that filter, and everything about issuing/validating tokens, is built in
 * the Authentication phase, not here. Right now every non-public path requires *some* authenticated
 * principal, but nothing in this codebase yet knows how to produce one — so hitting a protected
 * endpoint today will correctly fail with 401 until the Auth phase adds the filter that populates
 * the {@code SecurityContext}.
 *
 * <p>Rationale for stateless sessions (FR-1.1-1.8): this is a horizontally scaled, multi-instance
 * system (FR-22.x) — session state living in one instance's memory would break the moment the load
 * balancer routes a follow-up request to a different instance. JWTs + Redis (for
 * blacklisting/refresh tracking, added in the Auth phase) avoid that entirely.
 */
@Configuration
public class SecurityConfig {

  /**
   * Paths that never require authentication. Kept as a single list here — rather than scattered
   * {@code permitAll()} calls across modules — so it's obvious at a glance what's actually public.
   */
  private static final String[] PUBLIC_PATHS = {
    "/api/v1/auth/**", "/actuator/health", "/actuator/health/**", "/actuator/info",
  };

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        // CSRF protection defends against browser-cookie-based session
        // riding; it's irrelevant for a stateless, JWT-bearer-token API
        // with no server-side session cookie, so it's disabled here
        // rather than requiring every client to fetch and send a CSRF
        // token for no security benefit.
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorize ->
                authorize.requestMatchers(PUBLIC_PATHS).permitAll().anyRequest().authenticated());

    // TODO(auth-phase): add JwtAuthenticationFilter here, e.g.
    //   http.addFilterBefore(jwtAuthenticationFilter,
    //       UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * Allowed origins are intentionally not "*" — a stricter, explicit list belongs here once the
   * frontend's deployed URL is known (local dev only for now). Revisit when the Deployment phase
   * gives us a real origin.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:3000"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(List.of("X-Trace-Id"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  /**
   * Registered now (rather than in the Auth phase) because it's a general-purpose, stateless bean
   * with no dependency on anything auth-specific — and having it available early means any code
   * that needs to hash something (e.g. a share-link password, FR-4.5) doesn't have to wait for the
   * Auth module to exist.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
