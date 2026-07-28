package com.docshare.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ensures every request has a correlation ID (trace ID), so a single request can be followed across
 * logs and, later, across service boundaries and OpenTelemetry traces (Phase 5: FR-20.3/20.5).
 *
 * <p>Behavior:
 *
 * <ul>
 *   <li>If the caller sent an {@code X-Trace-Id} header (e.g. a request forwarded by another
 *       internal service, or the frontend echoing one it already has), that ID is reused — this is
 *       what lets a trace survive across a chain of calls once we're multi-service.
 *   <li>Otherwise a new UUID is generated.
 *   <li>The ID is placed in the SLF4J {@link MDC} under {@code "traceId"}, so the console log
 *       pattern (see {@code application.yml}) and every {@link GlobalExceptionHandler} error
 *       response can include it.
 *   <li>The ID is echoed back on the response as {@code X-Trace-Id}, so the frontend can
 *       log/display it for support requests ("give us this ID when you report a bug").
 * </ul>
 *
 * <p>{@code @Order(Ordered.HIGHEST_PRECEDENCE)} ensures this runs before Spring Security's filters,
 * so even a request rejected by security still gets a traceId in its error response.
 */
@Component
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  public static final String TRACE_ID_HEADER = "X-Trace-Id";
  public static final String MDC_KEY = "traceId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = request.getHeader(TRACE_ID_HEADER);
    if (traceId == null || traceId.isBlank()) {
      traceId = UUID.randomUUID().toString();
    }

    MDC.put(MDC_KEY, traceId);
    response.setHeader(TRACE_ID_HEADER, traceId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      // MDC is thread-local and threads are reused (pooled) between
      // requests — clearing here prevents one request's traceId from
      // leaking into the next request that happens to reuse this thread.
      MDC.remove(MDC_KEY);
    }
  }
}
