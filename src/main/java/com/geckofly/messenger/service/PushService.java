package com.geckofly.messenger.service;

import com.geckofly.messenger.model.entity.DeviceTokenEntity;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.repository.DeviceTokenRepository;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Отправка push через FCM (R9). Если Firebase не сконфигурирован (бин FirebaseMessaging
 * отсутствует) — методы тихо ничего не делают. Мёртвые токены (UNREGISTERED) удаляются.
 *
 * Сообщение шлётся ТОЛЬКО как data (без notification-блока) — иначе Android рисует
 * уведомление сам, наш код в этот момент не участвует и не может потом ни объединить
 * несколько сообщений одного чата в одно уведомление, ни снять его при прочтении
 * (у нас просто нет ID того, что нарисовала система). При data-only это делает
 * PushService на телефоне (см. push_service.dart) — тем же способом, каким устроены
 * Telegram/WhatsApp. AndroidConfig.Priority.HIGH — чтобы фоновый обработчик успел
 * отработать и показать уведомление даже при закрытом приложении.
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
        Map<String, String> fullData = new HashMap<>(data);
        fullData.put("title", title);
        fullData.put("body", body);
        if (imageUrl != null && !imageUrl.isBlank()) {
            fullData.put("imageUrl", imageUrl);
        }
        List<DeviceTokenEntity> tokens = deviceTokenRepository.findByUser(user);
        for (DeviceTokenEntity token : tokens) {
            try {
                messaging.send(Message.builder()
                        .setToken(token.getToken())
                        .putAllData(fullData)
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
