package com.geckofly.messenger.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Инициализация Firebase для push (R9). Если путь к service-account JSON не задан
 * (messenger.firebase.credentials-path пуст) — бин не создаётся и push тихо отключён.
 * Так dev-окружение и прод без Firebase работают без ошибок.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${messenger.firebase.credentials-path:}")
    private String credentialsPath;

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.info("FCM disabled: messenger.firebase.credentials-path is not set");
            return null;
        }
        try (InputStream in = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
            log.info("FCM enabled");
            return FirebaseMessaging.getInstance(app);
        } catch (Exception e) {
            log.error("FCM init failed — push disabled", e);
            return null;
        }
    }
}
