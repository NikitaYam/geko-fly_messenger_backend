package com.ultima.messenger.model.dto.message;

import com.ultima.messenger.model.enums.MessageType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SendMessageRequest {

    @NotNull(message = "Chat ID is required")
    private Long chatId;

    @Size(max = 4096, message = "Content must be less than 4096 characters")
    private String content;

    private MessageType type;

    private List<AttachmentDto> attachments;
}
