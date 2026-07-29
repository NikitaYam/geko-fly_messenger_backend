package com.geckofly.messenger.websocket.dto;

import java.util.UUID;

/**
 * Нагрузка события MESSAGE_STATUS: пользователь userUuid в чате chatUuid продвинул
 * вотермарку до сообщения uptoMessageUuid со статусом status (DELIVERED|READ).
 * Клиент-отправитель помечает свои сообщения в этом чате вплоть до указанного — этим статусом.
 */
public record MessageStatusPayload(UUID chatUuid, UUID userUuid, UUID uptoMessageUuid, String status) {}
