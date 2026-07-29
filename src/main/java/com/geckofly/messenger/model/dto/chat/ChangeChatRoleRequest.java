package com.geckofly.messenger.model.dto.chat;

import com.geckofly.messenger.model.enums.UserChatRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Тело PATCH /api/chats/{uuid}/participants/{userUuid}/role. */
@Getter
@Setter
public class ChangeChatRoleRequest {

    @NotNull(message = "role is required")
    private UserChatRole role;
}
