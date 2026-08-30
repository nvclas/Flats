PRAGMA foreign_keys = OFF;

CREATE TABLE areas_new
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
    FOREIGN KEY (flat_name) REFERENCES flats (name) ON DELETE CASCADE ON UPDATE CASCADE
);

INSERT INTO areas_new
SELECT id,
       flat_name,
       world,
       min_x,
       min_y,
       min_z,
       max_x,
       max_y,
       max_z
FROM areas;
DROP TABLE areas;
ALTER TABLE areas_new
    RENAME TO areas;

CREATE TABLE trusted_new
(
    flat_name   TEXT NOT NULL,
    player_uuid TEXT NOT NULL,
    PRIMARY KEY (flat_name, player_uuid),
    FOREIGN KEY (flat_name) REFERENCES flats (name) ON DELETE CASCADE ON UPDATE CASCADE
);

INSERT INTO trusted_new
SELECT flat_name, player_uuid
FROM trusted;
DROP TABLE trusted;
ALTER TABLE trusted_new
    RENAME TO trusted;

CREATE INDEX IF NOT EXISTS idx_areas_flat_name ON areas (flat_name);
CREATE INDEX IF NOT EXISTS idx_areas_spatial ON areas (world, min_x, max_x, min_z, max_z);

PRAGMA foreign_keys = ON;
PRAGMA foreign_key_check;
