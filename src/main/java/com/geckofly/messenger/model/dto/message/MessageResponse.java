package com.geckofly.messenger.model.dto.message;

import com.geckofly.messenger.model.dto.user.UserSummary;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class MessageResponse {
    private UUID uuid;
    private UUID chatUuid;
    private UserSummary sender;
    private String content;
    private String type;
    private Instant createdAt;
    private List<AttachmentDto> attachments;
    /** Статус для отправителя (SENT/DELIVERED/READ); null для входящих сообщений. */
    private String status;
    private Instant editedAt;
    private boolean deleted;
}
