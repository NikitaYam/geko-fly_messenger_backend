package com.geckofly.messenger.websocket.interceptor;

import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.repository.UserRepository;
import com.geckofly.messenger.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Перехватывает входящие STOMP-фреймы на уровне messaging-канала.
 *
 * Зачем нужен, если уже есть JwtHandshakeInterceptor?
 * Браузерный WebSocket API запрещает устанавливать произвольные HTTP-заголовки
 * при рукопожатии — это ограничение спецификации W3C. Поэтому браузерные клиенты
 * передают JWT не в HTTP-заголовке, а в заголовках STOMP CONNECT фрейма
 * (connectHeaders в @stomp/stompjs).
 *
 * Логика:
 * 1. Если пользователь уже есть в атрибутах сессии (поставлен JwtHandshakeInterceptor
 *    при HTTP-рукопожатии — сценарий Postman) — пропускаем без повторной валидации.
 * 2. Иначе читаем Authorization из заголовков STOMP CONNECT фрейма,
 *    валидируем JWT и кладём UserEntity в атрибуты сессии — браузерный сценарий.
 * 3. Если токен отсутствует или невалиден — бросаем исключение, соединение закрывается.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();

        // Пользователь уже аутентифицирован на уровне HTTP-рукопожатия (Postman)
        if (sessionAttrs != null && sessionAttrs.get("user") != null) {
            UserEntity existing = (UserEntity) sessionAttrs.get("user");
            // Устанавливаем Principal — нужен для convertAndSendToUser
            accessor.setUser(new UsernamePasswordAuthenticationToken(existing.getLogin(), null));
            log.debug("STOMP CONNECT: Principal set from handshake for user={}", existing.getLogin());
            return message;
        }

        // Браузерный клиент: читаем JWT из заголовков STOMP CONNECT фрейма
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("STOMP CONNECT rejected: no Bearer token in STOMP headers");
            throw new BadCredentialsException("Authorization header missing in STOMP CONNECT");
        }

        String token = authHeader.substring(7);

        try {
            String login = jwtService.extractUsername(token);
            UserEntity user = userRepository.findByLogin(login)
                    .orElseThrow(() -> new BadCredentialsException("User not found: " + login));

            if (!jwtService.validateToken(token, user)) {
                throw new BadCredentialsException("Invalid or expired JWT token");
            }

            if (sessionAttrs != null) {
                sessionAttrs.put("user", user);
            }

            // Principal — имя пользователя, по которому Spring маршрутизирует
            // сообщения при вызове convertAndSendToUser(login, ...)
            accessor.setUser(new UsernamePasswordAuthenticationToken(login, null));

            log.debug("STOMP CONNECT: authenticated user={} from STOMP headers", login);

        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.warn("STOMP CONNECT: token validation failed — {}", e.getMessage());
            throw new BadCredentialsException("JWT validation error: " + e.getMessage());
        }

        return message;
    }
}
