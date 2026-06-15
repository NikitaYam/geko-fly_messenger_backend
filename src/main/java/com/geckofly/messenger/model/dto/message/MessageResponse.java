package com.geckofly.messenger.model.dto.message;

import com.geckofly.messenger.model.dto.user.UserSummary;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
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
    private LocalDateTime createdAt;
    private List<AttachmentDto> attachments;
}
