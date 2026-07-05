CREATE TABLE user_defaults
(
    user_id           uuid   PRIMARY KEY REFERENCES users (id),
    default_pantry_id uuid   REFERENCES pantries (id) ON DELETE SET NULL,
    version           bigint NOT NULL
);
