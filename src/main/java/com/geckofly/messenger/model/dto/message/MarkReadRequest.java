package com.geckofly.messenger.model.dto.message;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Тело POST /api/chats/{chatUuid}/read — прочитано вплоть до этого сообщения. */
@Getter
@Setter
public class MarkReadRequest {

    @NotNull(message = "messageUuid is required")
    private UUID messageUuid;
}
