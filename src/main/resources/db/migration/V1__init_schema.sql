CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID         NOT NULL,
    login           VARCHAR(255) NOT NULL,
    user_email      VARCHAR(255),
    display_name    VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    public_key      VARCHAR(255),
    admin           BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP(6),
    updated_at      TIMESTAMP(6),
    CONSTRAINT uk_users_login UNIQUE (login),
    CONSTRAINT uk_users_uuid UNIQUE (uuid),
    CONSTRAINT uk_users_email UNIQUE (user_email)
);

CREATE INDEX IF NOT EXISTS idx_users_uuid ON users (uuid);

CREATE TABLE IF NOT EXISTS chats (
    id         BIGSERIAL PRIMARY KEY,
    chat_type  VARCHAR(255) NOT NULL,
    title      VARCHAR(255),
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS chat_participants (
    id         BIGSERIAL PRIMARY KEY,
    chat_id    BIGINT       NOT NULL REFERENCES chats (id),
    user_id    BIGINT       NOT NULL REFERENCES users (id),
    role       VARCHAR(255) NOT NULL,
    joined_at  TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    CONSTRAINT uk_chat_participants_chat_user UNIQUE (chat_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_participants_user ON chat_participants (user_id);
CREATE INDEX IF NOT EXISTS idx_participants_chat ON chat_participants (chat_id);

CREATE TABLE IF NOT EXISTS messages (
    id         BIGSERIAL PRIMARY KEY,
    chat_id    BIGINT        NOT NULL REFERENCES chats (id),
    sender_id  BIGINT        NOT NULL REFERENCES users (id),
    content    VARCHAR(4096) NOT NULL,
    type       VARCHAR(255)  NOT NULL,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6)
);

CREATE INDEX IF NOT EXISTS idx_messages_chat_id ON messages (chat_id);
CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON messages (sender_id);

CREATE TABLE IF NOT EXISTS attachments (
    id         BIGSERIAL PRIMARY KEY,
    message_id BIGINT       NOT NULL REFERENCES messages (id),
    file_url   VARCHAR(255) NOT NULL,
    file_name  VARCHAR(255) NOT NULL,
    file_type  VARCHAR(255) NOT NULL,
    file_size  BIGINT       NOT NULL,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    token      VARCHAR(512) NOT NULL,
    user_id    BIGINT       NOT NULL REFERENCES users (id),
    expires_at TIMESTAMP(6) NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token)
);
