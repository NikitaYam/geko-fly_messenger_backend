alter table users drop column if exists public_key;

alter index if exists idx_chats_uuiid rename to idx_chats_uuid;
alter index if exists idx_messages_uuiid rename to idx_messages_uuid;