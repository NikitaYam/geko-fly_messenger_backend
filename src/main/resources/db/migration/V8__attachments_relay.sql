-- R4: relay файлов. attachments получают stored_name (имя на диске) и флаг
-- file_deleted; отдельная таблица учёта, кто из получателей уже скачал файл.

alter table attachments add column if not exists stored_name varchar(255);

-- Бэкфилл для существующих строк: последний сегмент file_url ("/api/files/xxx.png" -> "xxx.png").
update attachments set stored_name = substring(file_url from '[^/]+$') where stored_name is null;

alter table attachments alter column stored_name set not null;

alter table attachments add column if not exists file_deleted boolean not null default false;

-- Кто из участников чата уже скачал (= получил) вложение.
create table if not exists attachment_deliveries (
    id            bigserial primary key,
    attachment_id bigint      not null references attachments (id),
    user_id       bigint      not null references users (id),
    delivered_at  timestamptz not null,
    constraint uk_attachment_delivery unique (attachment_id, user_id)
);

create index if not exists idx_att_deliveries_attachment on attachment_deliveries (attachment_id);
