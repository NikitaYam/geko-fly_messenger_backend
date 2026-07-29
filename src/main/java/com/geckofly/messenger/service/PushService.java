package com.geckofly.messenger.service;

import com.geckofly.messenger.model.entity.DeviceTokenEntity;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.repository.DeviceTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Отправка push через FCM (R9). Если Firebase не сконфигурирован (бин FirebaseMessaging
 * отсутствует) — методы тихо ничего не делают. Мёртвые токены (UNREGISTERED) удаляются.
 * Data-message: клиент сам решает, как показать (учитывая настройки звука/бейджа).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;
    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional
    public void pushToUser(UserEntity user, Map<String, String> data) {
        FirebaseMessaging messaging = firebaseMessagingProvider.getIfAvailable();
        if (messaging == null) {
            return; // push отключён
        }
        List<DeviceTokenEntity> tokens = deviceTokenRepository.findByUser(user);
        for (DeviceTokenEntity token : tokens) {
            try {
                messaging.send(Message.builder()
                        .setToken(token.getToken())
                        .putAllData(data)
                        .build());
            } catch (FirebaseMessagingException e) {
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    // Приложение удалено/токен протух — чистим.
                    deviceTokenRepository.deleteByToken(token.getToken());
                    log.debug("Removed stale FCM token for user {}", user.getLogin());
                } else {
                    log.warn("FCM send failed for user {}: {}", user.getLogin(), e.getMessage());
                }
            }
        }
    }
}
