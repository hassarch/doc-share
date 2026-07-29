package com.docshare.backend.config;

import com.docshare.backend.auth.service.JwtAuthenticationFilter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * The shape of request authorization for this application — which paths are public, which require
 * authentication, and the session/CORS/CSRF posture.
 *
 * <p>JWT validation is now wired through {@link JwtAuthenticationFilter}, which extracts tokens
 * from the {@code Authorization: Bearer <token>} header and populates Spring Security's {@code
 * SecurityContext}. Per FR-1.7, all endpoints except {@code /auth/*} require a valid JWT — this is
 * enforced by the combination of the filter (which produces an authenticated principal) and the
 * security chain below (which demands {@code authenticated()} for non-public paths).
 *
 * <p>Rationale for stateless sessions (FR-1.1-1.8): this is a horizontally scaled, multi-instance
 * system (FR-22.x) — session state living in one instance's memory would break the moment the load
 * balancer routes a follow-up request to a different instance. JWTs + Redis (for refresh token
 * tracking) avoid that entirely.
 */
@Configuration
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  /**
   * Paths that never require authentication. Kept as a single list here — rather than scattered
   * {@code permitAll()} calls across modules — so it's obvious at a glance what's actually public.
   */
  private static final String[] PUBLIC_PATHS = {
    "/api/v1/auth/**", "/actuator/health", "/actuator/health/**", "/actuator/info",
  };

  public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
  }

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
                authorize.requestMatchers(PUBLIC_PATHS).permitAll().anyRequest().authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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
