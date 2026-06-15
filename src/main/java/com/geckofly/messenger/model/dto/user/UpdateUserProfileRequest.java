package com.geckofly.messenger.model.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserProfileRequest {

    @Size(min=1, max=128)
    private String displayName;

    @Email
    @Size(max=256)
    private String email;

    @Size(max=512)
    private String avatarUrl; 
}
