package com.geckofly.messenger.websocket.interceptor;

import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.repository.UserRepository;
import com.geckofly.messenger.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Перехватывает HTTP-рукопожатие перед установкой WebSocket-соединения.
 *
 * Всегда разрешает соединение (возвращает true), потому что браузерный
 * WebSocket API не позволяет устанавливать произвольные HTTP-заголовки
 * при рукопожатии — это ограничение спецификации W3C.
 *
 * Браузерные клиенты аутентифицируются позже через StompAuthChannelInterceptor,
 * который читает JWT из заголовков STOMP CONNECT фрейма.
 *
 * Если JWT всё же передан в HTTP-заголовке (например, Postman),
 * пользователь сохраняется в атрибутах сессии сразу, и STOMP-перехватчик
 * пропустит его без повторной валидации.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        String authHeader = request.getHeaders().getFirst("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String login = jwtService.extractUsername(token);
                UserEntity user = userRepository.findByLogin(login).orElse(null);
                if (user != null && jwtService.validateToken(token, user)) {
                    attributes.put("user", user);
                    log.debug("WebSocket handshake: user={} authenticated from HTTP header", login);
                }
            } catch (Exception e) {
                log.debug("WebSocket handshake: could not parse HTTP Authorization header — {}", e.getMessage());
            }
        }

        // Всегда разрешаем: браузерные клиенты аутентифицируются
        // через StompAuthChannelInterceptor при получении STOMP CONNECT фрейма.
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }
}
