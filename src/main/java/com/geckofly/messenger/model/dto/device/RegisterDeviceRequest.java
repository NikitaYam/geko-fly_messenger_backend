package com.geckofly.messenger.model.dto.device;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Тело POST /api/devices — регистрация FCM-токена устройства. */
@Getter
@Setter
public class RegisterDeviceRequest {

    @NotBlank(message = "token is required")
    private String token;

    /** android | ios | web (необязательно). */
    private String platform;
}
