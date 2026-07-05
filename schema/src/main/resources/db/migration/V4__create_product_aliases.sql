CREATE TABLE product_aliases
(
    pantry_id  uuid NOT NULL REFERENCES pantries (id) ON DELETE CASCADE,
    alias      text NOT NULL,
    product_id uuid NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    PRIMARY KEY (pantry_id, alias)
);
CREATE INDEX ix_product_aliases_product_id ON product_aliases (product_id);
