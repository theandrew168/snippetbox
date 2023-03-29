CREATE TABLE IF NOT EXISTS session (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    token TEXT NOT NULL,
    data TEXT NOT NULL,
    expires TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS session_expires_idx ON session (expires);