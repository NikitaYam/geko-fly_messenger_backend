package com.geckofly.messenger.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Какой чат сейчас открыт на экране у пользователя — по логину, как и
 * PresenceService (одно значение на пользователя, не на устройство/сессию:
 * этого достаточно для типичного одного активного устройства и держит модель
 * простой). В БД не пишем: как и presence, восстанавливается само —
 * клиент присылает актуальный активный чат заново при каждом подключении.
 *
 * Нужно, чтобы не слать push по чату, который человек и так видит на экране,
 * но при этом слать push по ВСЕМ остальным чатам, даже если приложение открыто
 * (в отличие от прежней логики "получатель вообще онлайн — push не шлём").
 */
@Service
public class ActiveChatService {

    private final Map<String, UUID> activeChatByLogin = new ConcurrentHashMap<>();

    public void setActiveChat(String login, UUID chatUuid) {
        if (chatUuid == null) {
            activeChatByLogin.remove(login);
        } else {
            activeChatByLogin.put(login, chatUuid);
        }
    }

    public void clear(String login) {
        activeChatByLogin.remove(login);
    }

    public boolean isViewing(String login, UUID chatUuid) {
        return chatUuid != null && chatUuid.equals(activeChatByLogin.get(login));
    }
}
