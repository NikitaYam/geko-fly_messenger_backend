-- R12 (Б4): все временные колонки переводятся в timestamptz (UTC).
-- JVM и Postgres в контейнерах работают в UTC (проверено на VPS 2026-07-19),
-- поэтому существующие наивные значения — это и есть UTC: конверсия
-- "at time zone 'utc'" сохраняет фактический момент времени точно.

alter table users
    alter column created_at type timestamptz using created_at at time zone 'utc',
    alter column updated_at type timestamptz using updated_at at time zone 'utc';

alter table chats
    alter column created_at type timestamptz using created_at at time zone 'utc',
    alter column updated_at type timestamptz using updated_at at time zone 'utc';

alter table chat_participants
    alter column joined_at  type timestamptz using joined_at  at time zone 'utc',
    alter column created_at type timestamptz using created_at at time zone 'utc',
    alter column updated_at type timestamptz using updated_at at time zone 'utc';

alter table messages
    alter column created_at type timestamptz using created_at at time zone 'utc',
    alter column updated_at type timestamptz using updated_at at time zone 'utc';

alter table attachments
    alter column created_at type timestamptz using created_at at time zone 'utc',
    alter column updated_at type timestamptz using updated_at at time zone 'utc';

alter table refresh_tokens
    alter column expires_at type timestamptz using expires_at at time zone 'utc',
    alter column created_at type timestamptz using created_at at time zone 'utc',
    alter column updated_at type timestamptz using updated_at at time zone 'utc';
