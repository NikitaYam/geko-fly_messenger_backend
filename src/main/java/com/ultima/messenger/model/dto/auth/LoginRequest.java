package com.ultima.messenger.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class LoginRequest {

    @Size(min = 3, max = 50, message = "Login must be between 3 and 50 characters")
    @NotBlank(message = "Login is required")
    private String login;

    @NotBlank(message = "Password is required")
    private String password;
    
}
