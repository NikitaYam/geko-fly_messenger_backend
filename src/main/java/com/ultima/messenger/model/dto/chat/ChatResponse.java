package com.ultima.messenger.model.dto.chat;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ChatResponse {

    private Long chatId;
    private String type;
    private String title;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private String lastSenderDisplayName;
    private String lastSenderAvatarUrl;
}
