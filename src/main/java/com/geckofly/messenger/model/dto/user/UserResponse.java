package com.geckofly.messenger.model.dto.user;

import com.geckofly.messenger.model.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UserResponse {
    private UUID uuid;
    private String login;
    private String displayName;
    private String email;
    private UserRole role;
    private String avatarUrl;
}
