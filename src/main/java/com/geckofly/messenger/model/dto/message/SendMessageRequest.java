package com.geckofly.messenger.model.dto.message;

import com.geckofly.messenger.model.enums.MessageType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SendMessageRequest {

    @NotNull(message = "Chat UUID is required")
    private UUID chatUuid;

    @Size(max = 4096, message = "Content must be less than 4096 characters")
    private String content;

    private MessageType type;

    private List<AttachmentDto> attachments;
}
