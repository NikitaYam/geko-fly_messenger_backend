package com.ultima.messenger.model.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateUserRoleRequest {

    @NotNull
    private Boolean admin;
}
