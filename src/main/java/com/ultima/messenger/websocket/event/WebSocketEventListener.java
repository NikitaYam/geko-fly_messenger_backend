package com.ultima.messenger.websocket.event;

import com.ultima.messenger.model.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Слушает системные события WebSocket-жизненного цикла.
 *
 * Сейчас выполняет только логирование — это даёт видимость в логах,
 * кто подключается и отключается. В Этапе 5+ здесь появится
 * обновление поля online/offline в UserEntity.
 */
@Slf4j
@Component
public class WebSocketEventListener {

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        UserEntity user = getUserFromSession(accessor);
        if (user != null) {
            log.info("WebSocket connected: user={}, sessionId={}", user.getLogin(), accessor.getSessionId());
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        UserEntity user = getUserFromSession(accessor);
        if (user != null) {
            log.info("WebSocket disconnected: user={}, sessionId={}", user.getLogin(), accessor.getSessionId());
        } else {
            log.info("WebSocket disconnected: unknown user, sessionId={}", accessor.getSessionId());
        }
    }

    private UserEntity getUserFromSession(StompHeaderAccessor accessor) {
        if (accessor.getSessionAttributes() == null) return null;
        return (UserEntity) accessor.getSessionAttributes().get("user");
    }
}
