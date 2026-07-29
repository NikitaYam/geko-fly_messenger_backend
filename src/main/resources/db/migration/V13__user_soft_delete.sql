-- R10: мягкое удаление (анонимизация) пользователя. Строка остаётся как «тумбстоун»,
-- сообщения и история чатов сохраняются, но войти под таким аккаунтом нельзя.
alter table users add column if not exists deleted boolean not null default false;
