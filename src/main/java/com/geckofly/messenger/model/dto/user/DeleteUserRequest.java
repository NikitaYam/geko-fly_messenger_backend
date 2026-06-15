package com.geckofly.messenger.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteUserRequest {

    @NotBlank(message = "Password is required for account deletion")
    private String password;
}
