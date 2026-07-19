package com.geckofly.messenger.service;

import com.geckofly.messenger.model.dto.auth.AuthResponse;
import com.geckofly.messenger.model.dto.auth.LoginRequest;
import com.geckofly.messenger.model.dto.auth.LogoutRequest;
import com.geckofly.messenger.model.dto.auth.RefreshTokenRequest;
import com.geckofly.messenger.model.entity.RefreshTokenEntity;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.repository.RefreshTokenRepository;
import com.geckofly.messenger.repository.UserRepository;
import com.geckofly.messenger.security.JwtService;
import com.geckofly.messenger.security.LoginAttemptService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;

    @Transactional
    public AuthResponse loginByLogin(LoginRequest request, String clientIp) {
        String rateKey = request.getLogin() + "|" + clientIp;
        loginAttemptService.checkAllowed(rateKey);

        UserEntity user = userRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> {
                    loginAttemptService.onFailure(rateKey);
                    return new BadCredentialsException("Invalid login or password");
                });

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getLogin(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            loginAttemptService.onFailure(rateKey);
            throw e;
        }

        loginAttemptService.onSuccess(rateKey);
        return issueTokens(user, "Login successful");
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String presented = request.getRefreshToken();

        // 1. Криптографическая проверка: подпись, срок, тип REFRESH.
        String login;
        try {
            login = jwtService.validateRefreshTokenAndGetUsername(presented);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        // 2. Токен должен существовать в БД (по хэшу). Если подпись валидна,
        //    а записи нет — токен уже был ротирован: либо гонка двух запросов,
        //    либо краденая копия. Логируем — это сигнал.
        RefreshTokenEntity stored = refreshTokenRepository.findByTokenHash(sha256(presented))
                .orElseThrow(() -> {
                    log.warn("Refresh token reuse detected for user '{}' — possible theft or race", login);
                    return new BadCredentialsException("Refresh token is no longer valid");
                });

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new BadCredentialsException("Refresh token expired");
        }

        // 3. Ротация: использованный токен погашен, выдаётся новая пара.
        UserEntity user = stored.getUser();
        refreshTokenRepository.delete(stored);
        return issueTokens(user, "Refresh successful");
    }

    /** Идемпотентный выход: неизвестный токен — не ошибка, сессии уже нет. */
    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenRepository.findByTokenHash(sha256(request.getRefreshToken()))
                .ifPresent(refreshTokenRepository::delete);
    }

    private AuthResponse issueTokens(UserEntity user, String message) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = jwtService.generateRefreshToken(user);

        // Гигиена вместо тотальной зачистки: удаляем только истёкшие токены.
        // Живые сессии других устройств (телефон + PWA) продолжают работать.
        refreshTokenRepository.deleteExpiredByUser(user, LocalDateTime.now());

        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setTokenHash(sha256(rawRefreshToken));
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(jwtService.getRefreshTokenExpiry());
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .message(message)
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .build();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}