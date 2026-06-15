package com.geckofly.messenger.model.dto.admin;

import com.geckofly.messenger.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCreateUserRequest {

    @NotBlank
    @Size(min = 3, max = 64)
    private String login;

    @NotBlank
    @Size(min = 6, max = 128)
    private String password;

    @NotBlank
    @Size(max = 128)
    private String displayName;

    @Email
    private String email;

    private UserRole role;
}
