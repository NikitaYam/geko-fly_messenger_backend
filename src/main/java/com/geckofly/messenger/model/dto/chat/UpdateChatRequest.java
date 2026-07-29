package com.geckofly.messenger.model.dto.chat;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Тело PATCH /api/chats/{uuid} — переименование группы / смена аватара. Поля опциональны. */
@Getter
@Setter
public class UpdateChatRequest {

    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;

    private String avatarUrl;
}
