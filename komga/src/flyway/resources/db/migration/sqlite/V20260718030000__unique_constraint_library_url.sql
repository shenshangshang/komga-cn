-- Add unique constraint on (LIBRARY_ID, URL) for SERIES and BOOK tables
-- to prevent duplicate records from concurrent scans.
CREATE UNIQUE INDEX IF NOT EXISTS uk__series__library_url ON SERIES (LIBRARY_ID, URL);
CREATE UNIQUE INDEX IF NOT EXISTS uk__book__library_url ON BOOK (LIBRARY_ID, URL);
