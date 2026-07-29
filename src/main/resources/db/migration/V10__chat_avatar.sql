-- R6: аватар группового чата.
alter table chats add column if not exists avatar_url varchar(255);
