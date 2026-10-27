-- The audit trail. Rows are written in their own transaction (REQUIRES_NEW) so a failed checkout cannot erase them.
CREATE TABLE audit_records (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    action      varchar(64)  NOT NULL,
    detail      varchar(500) NOT NULL,
    recorded_at timestamptz  NOT NULL
);
COMMENT ON TABLE audit_records IS 'Append-only record of what was attempted; never holds secrets or personal data beyond ids.';
CREATE INDEX audit_records_recorded_at_idx ON audit_records (recorded_at DESC);
