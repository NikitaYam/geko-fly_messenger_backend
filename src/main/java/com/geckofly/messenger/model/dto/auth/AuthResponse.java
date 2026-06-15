package com.geckofly.messenger.model.dto.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthResponse {

    private String message;
    private String accessToken;
    private String refreshToken;
}
