package com.geckofly.messenger.websocket.config;

import com.geckofly.messenger.websocket.interceptor.JwtHandshakeInterceptor;
import com.geckofly.messenger.websocket.interceptor.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-native")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }

    /**
     * /topic  — broadcast всем подписчикам топика.
     * /queue  — адресная доставка конкретному пользователю (user queue).
     * /app    — prefix для @MessageMapping методов.
     * /user   — prefix для user-destination; Spring автоматически разворачивает
     *           convertAndSendToUser("alice", "/queue/messages") в /user/alice/queue/messages.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Регистрирует StompAuthChannelInterceptor на входящем канале.
     * Валидирует JWT из STOMP CONNECT фрейма для браузерных клиентов.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
