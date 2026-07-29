package com.geckofly.messenger.model.dto.user;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Минимальная информация о пользователе для встраивания в другие ответы API.
 * Числовой id никогда не включается — только uuid как публичный идентификатор.
 */
@Getter
@Setter
@Builder
public class UserSummary {
    private UUID uuid;
    private String login;
    private String displayName;
    private String avatarUrl;
    /** Онлайн ли пользователь сейчас (nullable — заполняется там, где уместно). */
    private Boolean online;
}
