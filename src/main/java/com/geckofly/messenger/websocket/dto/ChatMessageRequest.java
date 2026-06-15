package com.geckofly.messenger.websocket.dto;

import com.geckofly.messenger.model.dto.message.AttachmentDto;
import com.geckofly.messenger.model.enums.MessageType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

import java.util.UUID;

/**
 * Сообщение, которое клиент отправляет через STOMP на /app/chat.sendMessage.
 * Десериализуется из JSON-тела STOMP-фрейма.
 */
@Getter
@Setter
public class ChatMessageRequest {

    private UUID chatUuid;
    private String content;
    private MessageType type;
    private List<AttachmentDto> attachments;
}
