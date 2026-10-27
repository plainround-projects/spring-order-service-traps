-- Units on hand per SKU. "version" is the optimistic-locking column: two checkouts racing for the last unit
-- cannot both win, because the second UPDATE finds a different version and matches no row.
CREATE TABLE stock_items (
    sku       varchar(64) PRIMARY KEY CHECK (length(btrim(sku)) > 0),
    available integer NOT NULL CHECK (available >= 0),
    version   bigint  NOT NULL DEFAULT 0
);
COMMENT ON TABLE stock_items IS 'Units on hand per SKU, guarded by an optimistic-lock version.';
