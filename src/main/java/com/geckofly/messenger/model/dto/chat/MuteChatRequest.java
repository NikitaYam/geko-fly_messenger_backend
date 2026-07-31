package com.geckofly.messenger.model.dto.chat;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Тело PATCH /api/chats/{uuid}/mute. */
@Getter
@Setter
public class MuteChatRequest {

    @NotNull(message = "muted is required")
    private Boolean muted;
}
