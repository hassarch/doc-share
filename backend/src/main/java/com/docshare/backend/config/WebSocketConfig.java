package com.docshare.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket setup for FR-12.6 (real-time notification badge updates). {@code /ws} is the
 * connection endpoint; {@code /user/queue/**} is where a specific user's notifications are pushed
 * via {@code SimpMessagingTemplate.convertAndSendToUser(userId, "/queue/notifications", payload)} —
 * Spring resolves "the user" from the {@code Principal} attached during the handshake by {@link
 * JwtHandshakeInterceptor}.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
  private final PrincipalHandshakeHandler principalHandshakeHandler;
  private final String[] allowedOrigins;

  public WebSocketConfig(
      JwtHandshakeInterceptor jwtHandshakeInterceptor,
      PrincipalHandshakeHandler principalHandshakeHandler,
      @Value("${docshare.cors.allowed-origins}") String allowedOrigins) {
    this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    this.principalHandshakeHandler = principalHandshakeHandler;
    this.allowedOrigins = allowedOrigins.split(",");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/ws")
        .addInterceptors(jwtHandshakeInterceptor)
        .setHandshakeHandler(principalHandshakeHandler)
        .setAllowedOrigins(allowedOrigins) // same docshare.cors.allowed-origins as SecurityConfig
        .withSockJS();
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic", "/queue");
    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
  }
}
