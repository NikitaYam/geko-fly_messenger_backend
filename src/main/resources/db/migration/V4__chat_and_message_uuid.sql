--Публичные uuid для чатов и сообщений

alter table chats add column if not exists uuid UUID;

update chats set uuid = gen_random_uuid() where uuid is null;

alter table chats alter column uuid set not null;

alter table chats add constraint uk_chats_uuid UNIQUE (uuid);

create index if not exists idx_chats_uuiid on chats (uuid);


alter table messages add column if not exists uuid UUID;

update messages set uuid = gen_random_uuid() where uuid is null;

alter table messages alter column uuid set not null;

alter table messages add constraint uk_messages_uuid UNIQUE (uuid);

create index if not exists idx_messages_uuiid on messages (uuid);
