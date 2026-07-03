CREATE TABLE receipt_drafts
(
    id         uuid        PRIMARY KEY,
    pantry_id  uuid        NOT NULL REFERENCES pantries (id),
    status     text        NOT NULL,
    created_at timestamptz NOT NULL,
    version    bigint      NOT NULL
);
CREATE INDEX ix_receipt_drafts_pantry_id ON receipt_drafts (pantry_id);

CREATE TABLE receipt_draft_lines
(
    id             uuid PRIMARY KEY,
    draft_id       uuid NOT NULL REFERENCES receipt_drafts (id),
    raw_text       text NOT NULL,
    action         text NOT NULL,
    product_id     uuid REFERENCES products (id),
    proposed_name  text,
    proposed_brand text,
    quantity       int  NOT NULL,
    confidence     text NOT NULL,
    expires_at     date,
    version        bigint NOT NULL
);
CREATE INDEX ix_receipt_draft_lines_draft_id ON receipt_draft_lines (draft_id);
