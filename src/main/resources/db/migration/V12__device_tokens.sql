-- R9: FCM-токены устройств для push. У пользователя может быть несколько устройств.
create table if not exists device_tokens (
    id         bigserial primary key,
    user_id    bigint       not null references users (id),
    token      varchar(512) not null,
    platform   varchar(32),
    created_at timestamptz,
    updated_at timestamptz,
    constraint uk_device_tokens_token unique (token)
);

create index if not exists idx_device_tokens_user on device_tokens (user_id);
