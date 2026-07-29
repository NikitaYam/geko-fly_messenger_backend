-- R5c: статусы сообщений «доставлено/прочитано» через вотермарки на пару (чат, пользователь).
-- Экономно: одна строка на участника чата вместо строки на каждое сообщение.
-- last_delivered_message_id / last_read_message_id — внутренний id сообщения (монотонен = хронология).

create table if not exists chat_read_state (
    id                        bigserial primary key,
    chat_id                   bigint not null references chats (id),
    user_id                   bigint not null references users (id),
    last_delivered_message_id bigint,
    last_read_message_id      bigint,
    constraint uk_chat_read_state unique (chat_id, user_id)
);

create index if not exists idx_read_state_chat on chat_read_state (chat_id);
