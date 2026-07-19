package com.geckofly.messenger.model.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ParticipantResponse {
    private UUID uuid;
    private String login;
    private String displayName;
    private String avatarUrl;
    private String email;
}
