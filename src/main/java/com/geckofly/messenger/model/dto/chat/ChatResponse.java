package com.geckofly.messenger.model.dto.chat;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ChatResponse {

    private UUID uuid;
    private String type;
    private String title;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private String lastSenderDisplayName;
    private String lastSenderAvatarUrl;
}
