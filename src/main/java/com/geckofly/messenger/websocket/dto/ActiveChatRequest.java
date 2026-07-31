package com.geckofly.messenger.websocket.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Сигнал клиента через STOMP на /app/chat.active: какой чат сейчас открыт на
 * экране (null/отсутствие поля — ни один, экран чата закрыт).
 */
@Getter
@Setter
public class ActiveChatRequest {

    private UUID chatUuid;
}
