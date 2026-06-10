package com.ultima.messenger.model.dto.user;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class PartisipantResponse {
    private UUID uuid;
    private String login;
    private String displayName;
    private String avatarUrl;
    private String email;
}
