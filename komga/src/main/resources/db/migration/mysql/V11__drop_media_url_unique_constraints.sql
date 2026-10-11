-- Drop the UNIQUE (LIBRARY_ID, URL) constraints on BOOK/SERIES.
--
-- The constraint is stricter than Komga's data model. A soft-deleted row keeps its URL, so the
-- delete/restore flow and the grouping-mode migration legitimately hold a live row whose URL
-- still exists on a soft-deleted row, which made scans fail with duplicate-key errors.
-- Upstream Komga has no such constraint and prevents duplicate rows via the per-library scan
-- lock (LibraryContentLifecycle.withLibraryScanLock), so drop it here too.
--
-- Conditional: a no-op when the index is already absent (e.g. dropped out-of-band), so the
-- migration cannot fail at startup. MySQL has no DROP INDEX IF EXISTS.

SET @drop_book := (SELECT IF(COUNT(*) > 0,
  'ALTER TABLE BOOK DROP INDEX uk__book__library_url', 'DO 0')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'BOOK' AND INDEX_NAME = 'uk__book__library_url');
PREPARE stmt FROM @drop_book;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_series := (SELECT IF(COUNT(*) > 0,
  'ALTER TABLE SERIES DROP INDEX uk__series__library_url', 'DO 0')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'SERIES' AND INDEX_NAME = 'uk__series__library_url');
PREPARE stmt FROM @drop_series;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
