package com.geckofly.messenger.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Конфигурация CORS (A7 из AUDIT.md).
 * Список источников берётся из messenger.cors.allowed-origins (по умолчанию "*").
 * На прод задать конкретные домены клиента/PWA через переменную окружения —
 * менять код не нужно. Мобильное приложение CORS не использует (это браузерный механизм).
 */
@Configuration
public class WebConfig {

    @Value("${messenger.cors.allowed-origins:*}")
    private String[] allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns(allowedOrigins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
