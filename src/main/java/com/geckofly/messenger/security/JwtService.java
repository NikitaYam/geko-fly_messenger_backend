package com.geckofly.messenger.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String CLAIM_TOKEN_TYPE = "typ";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(userDetails, accessTokenExpiration, TokenType.ACCESS);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails, refreshTokenExpiration, TokenType.REFRESH);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public LocalDateTime getRefreshTokenExpiry() {
        return LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000);
    }

    /**
     * Проверка для REST-фильтра и WebSocket-перехватчиков.
     * Принимает ТОЛЬКО access-токены: refresh, предъявленный как Bearer,
     * здесь отсеется по claim "typ".
     */
    public boolean validateAccessToken(String token, UserDetails userDetails) {
        Claims claims = parseClaims(token);
        return userDetails.getUsername().equals(claims.getSubject())
                && TokenType.ACCESS.name().equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    /**
     * Проверка для /api/auth/refresh: подпись, срок и тип REFRESH.
     * Возвращает логин из токена; на любую проблему бросает JwtException.
     */
    public String validateRefreshTokenAndGetUsername(String token) {
        Claims claims = parseClaims(token);
        if (!TokenType.REFRESH.name().equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new JwtException("Not a refresh token");
        }
        return claims.getSubject();
    }

    private String buildToken(UserDetails userDetails, long expiration, TokenType type) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim(CLAIM_TOKEN_TYPE, type.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(parseClaims(token));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}