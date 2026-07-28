package com.docshare.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @Test
  void generatesTraceIdWhenCallerProvidesNone() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = (req, res) -> {};

    filter.doFilter(request, response, chain);

    String traceId = response.getHeader(CorrelationIdFilter.TRACE_ID_HEADER);
    assertThat(traceId).isNotBlank();
  }

  @Test
  void reusesCallerProvidedTraceId() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(CorrelationIdFilter.TRACE_ID_HEADER, "caller-supplied-id-123");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = (req, res) -> {};

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(CorrelationIdFilter.TRACE_ID_HEADER))
        .isEqualTo("caller-supplied-id-123");
  }

  @Test
  void clearsMdcAfterRequestCompletes() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = (req, res) -> {};

    filter.doFilter(request, response, chain);

    // Must be cleared — threads are pooled and reused across requests; a
    // stale traceId left in MDC would leak into the next unrelated request.
    assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
  }
}
