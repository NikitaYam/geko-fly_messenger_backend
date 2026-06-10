package com.ultima.messenger.model.dto.message;

import com.ultima.messenger.model.dto.user.UserSummary;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class MessageResponse {
    private Long id;
    private Long chatId;
    private UserSummary sender;
    private String content;
    private String type;
    private LocalDateTime createdAt;
    private List<AttachmentDto> attachments;
}
