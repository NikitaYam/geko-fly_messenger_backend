-- R2: refresh-токены хранятся как SHA-256 хэш + ротация при каждом refresh.

-- Старые токены записаны открытым текстом и выданы без claim "typ" -
-- обнуляем: все сессии сбрасываются, пользователи перелогинятся один раз.
truncate table refresh_tokens;

alter table refresh_tokens rename column token to token_hash;

-- SHA-256 в hex - ровно 64 символа.
alter table refresh_tokens alter column token_hash type varchar(64);

-- Имя constraint из V1: uk_refresh_tokens_token.
alter table refresh_tokens rename constraint uk_refresh_tokens_token to uk_refresh_tokens_token_hash;

-- Флаг revoked никогда не выставлялся кодом; отзыв = удаление строки.
alter table refresh_tokens drop column if exists revoked;