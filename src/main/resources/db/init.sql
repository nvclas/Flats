CREATE TABLE IF NOT EXISTS flats
(
    name       TEXT PRIMARY KEY,
    owner_uuid TEXT
);

CREATE TABLE IF NOT EXISTS areas
(
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    flat_name TEXT    NOT NULL,
    world     TEXT    NOT NULL,
    min_x     INTEGER NOT NULL,
    min_y     INTEGER NOT NULL,
    min_z     INTEGER NOT NULL,
    max_x     INTEGER NOT NULL,
    max_y     INTEGER NOT NULL,
    max_z     INTEGER NOT NULL,
    FOREIGN KEY (flat_name) REFERENCES flats (name) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS trusted
(
    flat_name   TEXT NOT NULL,
    player_uuid TEXT NOT NULL,
    PRIMARY KEY (flat_name, player_uuid),
    FOREIGN KEY (flat_name) REFERENCES flats (name) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_flats_owner_uuid ON flats (owner_uuid);
CREATE INDEX IF NOT EXISTS idx_areas_flat_name ON areas (flat_name);
CREATE INDEX IF NOT EXISTS idx_areas_spatial ON areas (world, min_x, max_x, min_z, max_z);
