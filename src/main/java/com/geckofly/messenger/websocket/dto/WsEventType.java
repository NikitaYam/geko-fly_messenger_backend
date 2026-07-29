package com.geckofly.messenger.websocket.dto;

/** Типы событий, летящих клиенту в конверте WsEnvelope по /user/queue/events. */
public enum WsEventType {
    CHAT_CREATED,      // тебя добавили в чат / создан чат с тобой
    MESSAGE_NEW,       // новое сообщение
    MESSAGE_EDITED,    // сообщение отредактировано
    MESSAGE_DELETED,   // сообщение удалено
    MESSAGE_STATUS,    // сдвиг доставлено/прочитано
    PRESENCE,          // пользователь онлайн/офлайн
    PARTICIPANT_ADDED, // в группу добавлен участник
    PARTICIPANT_REMOVED, // участник вышел/удалён
    CHAT_UPDATED       // сменились title/avatar/роль в группе
}
