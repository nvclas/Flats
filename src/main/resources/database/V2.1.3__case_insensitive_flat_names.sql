-- 1. Automatically resolve duplicates by appending underscores iteratively
WITH RECURSIVE
-- Find all duplicate names (ignoring case) keeping the first occurrence intact
duplicates AS (SELECT name AS      original_name,
                      ROW_NUMBER() OVER (
                          PARTITION BY LOWER(name)
                          ORDER BY ROWID
                          ) AS rn
               FROM flats),
-- Recursively append '_' until a free name is found
resolved (original_name, current_name) AS (SELECT original_name,
                                                  original_name || '_' AS current_name
                                           FROM duplicates
                                           WHERE rn > 1

                                           UNION ALL

                                           SELECT r.original_name,
                                                  r.current_name || '_'
                                           FROM resolved r
                                           WHERE EXISTS (SELECT 1
                                                         FROM flats f
                                                         WHERE LOWER(f.name) = LOWER(r.current_name))),
-- Select only the final unique name for each original row
final_names AS (SELECT original_name,
                       current_name,
                       ROW_NUMBER() OVER (
                           PARTITION BY original_name
                           ORDER BY LENGTH(current_name) DESC
                           ) AS max_rn
                FROM resolved)
UPDATE flats
SET name = fn.current_name FROM final_names fn
WHERE flats.name = fn.original_name
  AND fn.max_rn = 1;

-- 2. Create the unique index on the cleaned dataset
CREATE UNIQUE INDEX IF NOT EXISTS idx_flats_name_unique_nocase ON flats (name COLLATE NOCASE);
