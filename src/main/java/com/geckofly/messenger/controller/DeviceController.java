package com.geckofly.messenger.controller;

import com.geckofly.messenger.model.dto.device.RegisterDeviceRequest;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    // Регистрация FCM-токена (клиент вызывает при логине / обновлении токена).
    @PostMapping
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterDeviceRequest request,
            @AuthenticationPrincipal UserEntity currentUser) {
        deviceService.register(currentUser, request.getToken(), request.getPlatform());
        return ResponseEntity.noContent().build();
    }

    // Отвязка токена (при logout).
    @DeleteMapping
    public ResponseEntity<Void> unregister(@RequestParam String token) {
        deviceService.unregister(token);
        return ResponseEntity.noContent().build();
    }
}
