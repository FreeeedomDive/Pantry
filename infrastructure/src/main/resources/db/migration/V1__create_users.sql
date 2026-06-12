CREATE TABLE users
(
    id               uuid        PRIMARY KEY,
    telegram_user_id bigint      NOT NULL,
    created_at       timestamptz NOT NULL,
    version          bigint      NOT NULL
);

CREATE UNIQUE INDEX uq_users_telegram_user_id ON users (telegram_user_id);