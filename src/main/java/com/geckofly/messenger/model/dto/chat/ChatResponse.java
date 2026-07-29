package com.geckofly.messenger.model.dto.chat;

import com.geckofly.messenger.model.dto.user.UserSummary;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ChatResponse {

    private UUID uuid;
    private String type;
    private String title;
    private String lastMessage;
    private Instant lastMessageTime;
    private String lastSenderDisplayName;
    private String lastSenderAvatarUrl;
    private UserSummary otherParticipant;
    private String avatarUrl;
    private long unreadCount;
}