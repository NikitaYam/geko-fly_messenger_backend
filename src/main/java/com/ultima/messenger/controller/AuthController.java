package com.ultima.messenger.controller;

import com.ultima.messenger.model.dto.auth.AuthResponse;
import com.ultima.messenger.model.dto.auth.LoginEmailRequest;
import com.ultima.messenger.model.dto.auth.LoginRequest;
import com.ultima.messenger.model.dto.auth.LogoutRequest;
import com.ultima.messenger.model.dto.auth.RefreshTokenRequest;
import com.ultima.messenger.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/loginByEmail")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginEmailRequest request) {
        AuthResponse response = authService.loginByEmail(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/loginByLogin")
    public ResponseEntity<AuthResponse> loginByLogin(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.loginByLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.ok().build();
    }
}
