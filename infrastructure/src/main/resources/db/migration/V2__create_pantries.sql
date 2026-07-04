CREATE TABLE pantries
(
    id         uuid        PRIMARY KEY,
    name       text        NOT NULL,
    created_at timestamptz NOT NULL,
    version    bigint      NOT NULL
);

CREATE TABLE pantry_members
(
    pantry_id uuid        NOT NULL REFERENCES pantries (id) ON DELETE CASCADE,
    user_id   uuid        NOT NULL REFERENCES users (id),
    role      text        NOT NULL,
    joined_at timestamptz NOT NULL,
    version    bigint      NOT NULL,
    PRIMARY KEY (pantry_id, user_id)
);
CREATE INDEX ix_pantry_members_user_id ON pantry_members (user_id);