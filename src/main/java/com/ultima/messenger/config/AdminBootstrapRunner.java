package com.ultima.messenger.config;

import com.ultima.messenger.model.entity.UserEntity;
import com.ultima.messenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${messenger.admin.bootstrap-login:}")
    private String bootstrapLogin;

    @Value("${messenger.admin.bootstrap-password:}")
    private String bootstrapPassword;

    @Value("${messenger.admin.bootstrap-display-name:Administrator}")
    private String bootstrapDisplayName;

    @Override
    public void run(ApplicationArguments args) {
        if (bootstrapLogin == null || bootstrapLogin.isBlank()
                || bootstrapPassword == null || bootstrapPassword.isBlank()) {
            return;
        }
        if (userRepository.existsByAdminTrue()) {
            return;
        }

        UserEntity admin = new UserEntity();
        admin.setLogin(bootstrapLogin.trim());
        admin.setDisplayName(bootstrapDisplayName);
        admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        admin.setAdmin(true);
        userRepository.save(admin);

        log.info("Bootstrap administrator created with login '{}'", admin.getLogin());
    }
}
