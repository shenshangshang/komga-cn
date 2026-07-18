-- Linux file paths are case-sensitive. Keep display metadata case-insensitive,
-- but compare indexed media URLs byte-for-byte so distinct paths are not merged.
ALTER TABLE SERIES
  MODIFY COLUMN URL VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;

ALTER TABLE BOOK
  MODIFY COLUMN URL VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;
