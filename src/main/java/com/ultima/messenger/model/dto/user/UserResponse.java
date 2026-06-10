package com.ultima.messenger.model.dto.user;

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
    private boolean admin;
}
