package com.geckofly.messenger.service;

import com.geckofly.messenger.model.entity.DeviceTokenEntity;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.repository.DeviceTokenRepository;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
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
 *
 * Сообщение шлётся как notification + data одновременно:
 *  - notification (title/body) — Android сам покажет уведомление, когда приложение
 *    в фоне или закрыто; без этого блока системе нечего показывать, пока приложение
 *    не запущено, а data-only сообщения таким адресатам системой не отрисовываются;
 *  - data — маршрутная нагрузка (chatUuid/messageUuid), чтобы по тапу открыть нужный чат;
 *  - AndroidConfig.Priority.HIGH — просит FCM доставить немедленно, а не отложить.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;
    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional
    public void pushToUser(UserEntity user, String title, String body, String imageUrl, Map<String, String> data) {
        FirebaseMessaging messaging = firebaseMessagingProvider.getIfAvailable();
        if (messaging == null) {
            return; // push отключён
        }
        Notification.Builder notification = Notification.builder()
                .setTitle(title)
                .setBody(body);
        if (imageUrl != null && !imageUrl.isBlank()) {
            notification.setImage(imageUrl);
        }
        List<DeviceTokenEntity> tokens = deviceTokenRepository.findByUser(user);
        for (DeviceTokenEntity token : tokens) {
            try {
                messaging.send(Message.builder()
                        .setToken(token.getToken())
                        .setNotification(notification.build())
                        .putAllData(data)
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .build())
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
