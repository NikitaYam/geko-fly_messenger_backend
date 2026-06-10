package com.ultima.messenger.websocket.dto;

import com.ultima.messenger.model.dto.message.AttachmentDto;
import com.ultima.messenger.model.enums.MessageType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Сообщение, которое клиент отправляет через STOMP на /app/chat.sendMessage.
 * Десериализуется из JSON-тела STOMP-фрейма.
 */
@Getter
@Setter
public class ChatMessageRequest {

    private Long chatId;
    private String content;
    private MessageType type;
    private List<AttachmentDto> attachments;
}
