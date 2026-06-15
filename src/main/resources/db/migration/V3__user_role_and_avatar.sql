-- Роли пользователя вместо флага admin

alter table users
    add column role varchar(32) not null default 'USER';

alter table users
    add column if not exists avatar_url varchar(512);

update users
set role = 'ADMIN'
where admin = TRUE;

alter table users
    drop column if exists admin;

alter table users
    add constraint chk_users_role
        check (role in ('USER', 'ADMIN', 'SUPER_ADMIN'));
create index if not exists idx_users_role on users (role);
