package com.example.miro.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor;

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(webSocketAuthChannelInterceptor);
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry config) {
    config.addEndpoint("/ws")
        .setAllowedOriginPatterns(parseAllowedOriginPatterns());
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic");
    registry.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
    registration.setMessageSizeLimit(1024 * 1024 * 10); // 10 MB
    registration.setSendTimeLimit(20000);
    registration.setSendBufferSizeLimit(1024 * 1024 * 10); // 10 MB
  }

  @Override
  public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
    messageConverters.add(new ByteArrayMessageConverter());
    return false;
  }

  private String[] parseAllowedOriginPatterns() {
    String allowedOriginPatterns = "http://localhost:5173,http://miro-backend-twdw.onrender.com";
    return Arrays.stream(allowedOriginPatterns.split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .toArray(String[]::new);
  }
}