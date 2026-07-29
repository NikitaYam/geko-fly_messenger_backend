package com.geckofly.messenger.service;

import com.geckofly.messenger.model.entity.DeviceTokenEntity;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Регистрация/удаление FCM-токенов устройств (R9). */
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceTokenRepository deviceTokenRepository;

    /** Регистрация токена. Если токен уже есть — переносим на текущего пользователя (сменился логин на устройстве). */
    @Transactional
    public void register(UserEntity user, String token, String platform) {
        DeviceTokenEntity entity = deviceTokenRepository.findByToken(token)
                .orElseGet(DeviceTokenEntity::new);
        entity.setUser(user);
        entity.setToken(token);
        entity.setPlatform(platform);
        deviceTokenRepository.save(entity);
    }

    @Transactional
    public void unregister(String token) {
        deviceTokenRepository.deleteByToken(token);
    }
}
