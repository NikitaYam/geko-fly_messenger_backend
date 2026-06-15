package com.geckofly.messenger.model.dto.admin;

import com.geckofly.messenger.model.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateUserRoleRequest {

    @NotNull
    private UserRole role;
}
