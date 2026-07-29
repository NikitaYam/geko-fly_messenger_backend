package com.geckofly.messenger.model.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Тело POST /api/chats/{uuid}/participants — добавить участника по логину. */
@Getter
@Setter
public class AddParticipantRequest {

    @NotBlank(message = "login is required")
    private String login;
}
