package com.geckofly.messenger.model.dto.user;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Участник чата: UserSummary + почта + роль в чате + онлайн. Ответ GET /api/chats/{uuid}/participants. */
@Getter
@Setter
@Builder
public class ParticipantResponse {
    private UUID uuid;
    private String login;
    private String displayName;
    private String avatarUrl;
    private String email;
    /** Роль в этом чате: ADMIN | MEMBER. */
    private String chatRole;
    private Boolean online;
    /** Удалённый (анонимизированный) участник — клиент может пометить особо (A9). */
    private boolean deleted;
}
