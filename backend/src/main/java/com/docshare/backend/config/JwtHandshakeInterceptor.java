package com.docshare.backend.config;

import com.docshare.backend.auth.service.JwtService;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Validates JWT tokens for WebSocket handshakes. Browsers' native WebSocket API can't attach custom
 * headers (like {@code Authorization: Bearer ...}) to the handshake — only query parameters or
 * cookies. So the token travels as a query parameter on the {@code /ws?token=...} connection URL
 * instead, and this interceptor validates it and attaches the user's identity to the WebSocket
 * session attributes, where {@link PrincipalHandshakeHandler} will read it back out.
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

  private final JwtService jwtService;

  public JwtHandshakeInterceptor(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {
    String query = request.getURI().getQuery();
    if (query == null) {
      return false;
    }

    String token =
        UriComponentsBuilder.fromUriString("/?" + query).build().getQueryParams().getFirst("token");

    if (token == null) {
      return false;
    }

    try {
      UUID userId = jwtService.validateAndGetUserId(token);
      attributes.put("principal", (Principal) () -> userId.toString());
      return true;
    } catch (JwtService.JwtValidationException e) {
      return false;
    }
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {
    // no-op
  }
}
