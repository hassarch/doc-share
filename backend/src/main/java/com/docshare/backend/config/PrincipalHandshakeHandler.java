package com.docshare.backend.config;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * Spring's {@link DefaultHandshakeHandler} normally resolves the WebSocket session's {@link
 * Principal} from the underlying {@code HttpServletRequest.getPrincipal()} — which is null here,
 * since our JWT check happens in {@link JwtHandshakeInterceptor}, not through Spring Security's
 * usual servlet-filter authentication. This override reads the principal the interceptor placed in
 * the handshake {@code attributes} map instead, which is what makes {@code
 * SimpMessagingTemplate.convertAndSendToUser(...)} able to find this session later.
 */
@Component
public class PrincipalHandshakeHandler extends DefaultHandshakeHandler {

  @Override
  protected Principal determineUser(
      ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
    Object principal = attributes.get("principal");
    if (principal instanceof Principal p) {
      return p;
    }
    throw new HandshakeFailureException(
        "No principal attached by JwtHandshakeInterceptor — handshake should have been rejected"
            + " before reaching here");
  }
}
