package com.ultima.messenger.model.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Минимальная информация о пользователе для встраивания в другие ответы API.
 * Числовой id никогда не включается — только uuid как публичный идентификатор.
 */
@Getter
@Builder
public class UserSummary {
    private UUID uuid;
    private String login;
    private String displayName;
    private String avatarUrl;
}
