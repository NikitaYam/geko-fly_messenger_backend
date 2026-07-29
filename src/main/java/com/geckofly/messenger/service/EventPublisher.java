package com.geckofly.messenger.service;

import com.geckofly.messenger.websocket.dto.WsEnvelope;
import com.geckofly.messenger.websocket.dto.WsEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Единая точка отправки событий пользователю в его личную очередь /queue/events.
 * Spring разворачивает convertAndSendToUser(login, "/queue/events", ...)
 * в /user/{login}/queue/events — клиент подписывается на /user/queue/events.
 * Сервисы не трогают SimpMessagingTemplate напрямую — только через этот класс.
 */
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private static final String USER_EVENTS_QUEUE = "/queue/events";

    private final SimpMessagingTemplate messagingTemplate;

    public void toUser(String login, WsEventType type, Object payload) {
        messagingTemplate.convertAndSendToUser(login, USER_EVENTS_QUEUE, WsEnvelope.of(type, payload));
    }

    /** Одна и та же нагрузка нескольким пользователям (presence, статусы). */
    public void toUsers(Collection<String> logins, WsEventType type, Object payload) {
        for (String login : logins) {
            toUser(login, type, payload);
        }
    }
}
