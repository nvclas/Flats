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
