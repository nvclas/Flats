-- 1. Resolve case-insensitive duplicates deterministically.
--    In groups of names that differ only by case, the row with the lowest ROWID
--    keeps its original name. All other rows get a suffix based on their position
--    ('_dup1', '_dup2', etc.).
--    Since ROW_NUMBER() gives every duplicate a unique index,
--    renamed rows can never collide with each other.
WITH duplicates AS (SELECT name  AS original_name,
                           ROW_NUMBER() OVER (
                               PARTITION BY LOWER(name)
                               ORDER BY ROWID
                               ) AS rn
                    FROM flats)
UPDATE flats
SET name = flats.name || '_dup' || (d.rn - 1)
FROM duplicates d
WHERE flats.name = d.original_name
  AND d.rn > 1;

-- 2. Create the unique index enforcing case-insensitive uniqueness going forward.
CREATE UNIQUE INDEX IF NOT EXISTS idx_flats_name_unique_nocase ON flats (name COLLATE NOCASE);
