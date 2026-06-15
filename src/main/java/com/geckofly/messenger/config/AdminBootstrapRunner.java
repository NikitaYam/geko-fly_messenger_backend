package com.geckofly.messenger.config;

import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.model.enums.UserRole;
import com.geckofly.messenger.repository.UserRepository;
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
                    log.warn("Bootstrap super-admin skipped: ADMIN_LOGIN or ADMIN_PASSWORD is not set");
                    return;
        }
        if (userRepository.existsByRole(UserRole.SUPER_ADMIN)) {
            return;
        }

        UserEntity superAdmin = new UserEntity();
        superAdmin.setLogin(bootstrapLogin.trim());
        superAdmin.setDisplayName(bootstrapDisplayName);
        superAdmin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        superAdmin.setRole(UserRole.SUPER_ADMIN);
        userRepository.save(superAdmin);

        log.info("Bootstrap super-admin created with login '{}'", superAdmin.getLogin());
    }
}
