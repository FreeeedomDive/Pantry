CREATE TABLE pantry_invites
(
    token      uuid        PRIMARY KEY,
    pantry_id  uuid        NOT NULL REFERENCES pantries (id) ON DELETE CASCADE,
    created_by uuid        NOT NULL REFERENCES users (id),
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    version    bigint      NOT NULL
);
CREATE INDEX ix_pantry_invites_pantry_id ON pantry_invites (pantry_id);
