package com.geckofly.messenger.websocket.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Нагрузка событий MESSAGE_EDITED / MESSAGE_DELETED.
 * Для правки: deleted=false, content — новый текст, editedAt проставлен.
 * Для удаления: deleted=true, content="".
 */
public record MessageMutationPayload(
        UUID chatUuid,
        UUID messageUuid,
        String content,
        Instant editedAt,
        boolean deleted
) {}
