-- Личное заглушение уведомлений по конкретному чату (не влияет на других участников).
alter table chat_participants add column if not exists muted boolean not null default false;
