package com.geckofly.messenger.controller;

import com.geckofly.messenger.model.dto.auth.AuthResponse;
import com.geckofly.messenger.model.dto.auth.LoginRequest;
import com.geckofly.messenger.model.dto.auth.LogoutRequest;
import com.geckofly.messenger.model.dto.auth.RefreshTokenRequest;
import com.geckofly.messenger.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/loginByLogin")
    public ResponseEntity<AuthResponse> loginByLogin(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        AuthResponse response = authService.loginByLogin(request, httpServletRequest.getRemoteAddr());
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
