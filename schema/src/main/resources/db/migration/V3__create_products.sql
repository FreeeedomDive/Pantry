CREATE TABLE products
(
    id         uuid        PRIMARY KEY,
    pantry_id  uuid        NOT NULL REFERENCES pantries (id) ON DELETE CASCADE,
    name       text        NOT NULL,
    brand      text,
    created_at timestamptz NOT NULL,
    version    bigint      NOT NULL
);
CREATE INDEX ix_products_pantry_id ON products (pantry_id);

CREATE TABLE stock_items
(
    id           uuid        PRIMARY KEY,
    product_id   uuid        NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    quantity     int         NOT NULL CHECK (quantity > 0),
    purchased_at timestamptz NOT NULL,
    expires_at   date,
    version      bigint      NOT NULL
);
CREATE INDEX ix_stock_items_product_id ON stock_items (product_id);
