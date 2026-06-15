package com.geckofly.messenger.service;

import com.geckofly.messenger.model.dto.auth.AuthResponse;
import com.geckofly.messenger.model.dto.auth.LoginEmailRequest;
import com.geckofly.messenger.model.dto.auth.LoginRequest;
import com.geckofly.messenger.model.dto.auth.LogoutRequest;
import com.geckofly.messenger.model.dto.auth.RefreshTokenRequest;
import com.geckofly.messenger.model.entity.RefreshTokenEntity;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.repository.RefreshTokenRepository;
import com.geckofly.messenger.repository.UserRepository;
import com.geckofly.messenger.security.JwtService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse loginByEmail(LoginEmailRequest request) {
        UserEntity user = userRepository.findByUserEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getLogin(), request.getPassword())
        );

        return generateTokens(user, "Login successful");
    }

    @Transactional
    public AuthResponse loginByLogin(LoginRequest request) {
        UserEntity user = userRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> new IllegalArgumentException("Invalid login or password"));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getLogin(), request.getPassword())
        );

        return generateTokens(user, "Login successful");
    }

    private AuthResponse generateTokens(UserEntity user, String message) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = jwtService.generateRefreshToken(user);

        refreshTokenRepository.deleteByUser(user);

        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setToken(rawRefreshToken);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(jwtService.getRefreshTokenExpiry());
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .message(message)
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .build();
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        RefreshTokenEntity token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (token.isRevoked()) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token expired");
        }

        String accessToken = jwtService.generateAccessToken(token.getUser());
        return AuthResponse.builder()
                .message("Refresh token successful")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void logout(LogoutRequest request) {
        String refreshToken = request.getRefreshToken();
        RefreshTokenEntity token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        refreshTokenRepository.delete(token);
    }
}
