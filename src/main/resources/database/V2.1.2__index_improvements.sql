DROP INDEX IF EXISTS idx_flats_owner_uuid;
CREATE INDEX idx_flats_owner_uuid ON flats (owner_uuid)
    WHERE owner_uuid IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_flats_name_nocase ON flats (name COLLATE NOCASE);

DROP INDEX IF EXISTS idx_areas_spatial;
CREATE INDEX idx_areas_spatial
    ON areas (world, min_x, max_x, min_z, max_z, min_y, max_y, flat_name);

DROP INDEX IF EXISTS idx_areas_flat_name;
CREATE INDEX idx_areas_flat_name
    ON areas (flat_name, world, min_x, min_y, min_z, max_x, max_y, max_z);

ANALYZE;
