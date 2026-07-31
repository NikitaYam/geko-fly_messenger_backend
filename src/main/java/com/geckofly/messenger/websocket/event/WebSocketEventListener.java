package com.geckofly.messenger.websocket.event;

import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.repository.ChatParticipantRepository;
import com.geckofly.messenger.service.ActiveChatService;
import com.geckofly.messenger.service.EventPublisher;
import com.geckofly.messenger.service.PresenceService;
import com.geckofly.messenger.websocket.dto.PresencePayload;
import com.geckofly.messenger.websocket.dto.WsEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;

/**
 * Presence: на подключение шлём онлайн, на отключение — офлайн (если у пользователя
 * не осталось других сессий). Событие уходит только тем, кто состоит с ним в общем чате.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final ChatParticipantRepository chatParticipantRepository;
    private final PresenceService presenceService;
    private final ActiveChatService activeChatService;
    private final EventPublisher eventPublisher;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        UserEntity user = getUserFromSession(event);
        if (user != null) {
            log.debug("WS connected: {}", user.getLogin());
            broadcastPresence(user, true);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        UserEntity user = getUserFromSession(event);
        // A5: закрывающаяся сессия ещё в реестре — исключаем её по sessionId.
        // Офлайн шлём, только если других сессий не осталось.
        if (user != null && !presenceService.hasOtherSession(user.getLogin(), event.getSessionId())) {
            log.debug("WS disconnected (last session): {}", user.getLogin());
            activeChatService.clear(user.getLogin());
            broadcastPresence(user, false);
        }
    }

    private void broadcastPresence(UserEntity user, boolean online) {
        List<String> coParticipants = chatParticipantRepository.findCoParticipantLogins(user);
        if (!coParticipants.isEmpty()) {
            eventPublisher.toUsers(coParticipants, WsEventType.PRESENCE,
                    new PresencePayload(user.getUuid(), user.getLogin(), online));
        }
    }

    private UserEntity getUserFromSession(org.springframework.context.ApplicationEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(
                ((org.springframework.web.socket.messaging.AbstractSubProtocolEvent) event).getMessage());
        if (accessor.getSessionAttributes() == null) return null;
        return (UserEntity) accessor.getSessionAttributes().get("user");
    }
}
