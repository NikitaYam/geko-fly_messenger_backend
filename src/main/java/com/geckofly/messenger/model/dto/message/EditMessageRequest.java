package com.geckofly.messenger.model.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Тело PATCH /api/messages/{uuid} — новый текст сообщения. */
@Getter
@Setter
public class EditMessageRequest {

    @NotBlank(message = "Content is required")
    @Size(max = 4096, message = "Content must be less than 4096 characters")
    private String content;
}
