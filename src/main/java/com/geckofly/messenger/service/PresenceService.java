package com.geckofly.messenger.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

/**
 * Presence (онлайн/офлайн) поверх встроенного SimpUserRegistry Spring.
 * В БД ничего не пишем: при рестарте сервера все переподключаются, состояние
 * восстановится само. Пользователь онлайн, если есть хотя бы одна активная
 * WebSocket-сессия (несколько устройств — несколько сессий).
 */
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final SimpUserRegistry userRegistry;

    public boolean isOnline(String login) {
        SimpUser user = userRegistry.getUser(login);
        return user != null && !user.getSessions().isEmpty();
    }

    /**
     * Есть ли у пользователя активная сессия, КРОМE указанной (A5).
     * Нужно на disconnect: закрывающаяся сессия ещё числится в реестре, поэтому
     * её исключаем — иначе «офлайн» никогда не отправился бы при мультиустройствах.
     */
    public boolean hasOtherSession(String login, String excludeSessionId) {
        SimpUser user = userRegistry.getUser(login);
        if (user == null) {
            return false;
        }
        return user.getSessions().stream()
                .anyMatch(s -> !s.getId().equals(excludeSessionId));
    }
}
