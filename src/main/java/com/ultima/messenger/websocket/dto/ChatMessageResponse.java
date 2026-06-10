package com.ultima.messenger.websocket.dto;

import com.ultima.messenger.model.dto.message.AttachmentDto;
import com.ultima.messenger.model.dto.user.UserSummary;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сообщение, которое сервер рассылает подписчикам /topic/chat/{chatId}.
 * Содержит полную информацию об отправителе, чтобы клиенту не нужно
 * было делать отдельный REST-запрос для отображения имени и аватара.
 */
@Getter
@Builder
public class ChatMessageResponse {

    private Long messageId;
    private Long chatId;
    private UserSummary sender;
    private String content;
    private String type;
    private LocalDateTime createdAt;
    private List<AttachmentDto> attachments;
}
