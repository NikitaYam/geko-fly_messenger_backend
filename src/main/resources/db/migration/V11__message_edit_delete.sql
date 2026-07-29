-- R7: редактирование и удаление сообщений.
alter table messages add column if not exists edited_at timestamptz;
alter table messages add column if not exists deleted boolean not null default false;
