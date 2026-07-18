-- Add unique constraint on (LIBRARY_ID, URL) for SERIES and BOOK tables
-- to prevent duplicate records from concurrent scans.
-- URL column changed from TEXT to VARCHAR(500) to support indexing.
-- Max observed URL length: 283 chars; 500 provides ample headroom.
-- Index size: 255*4 + 500*4 = 3020 bytes < 3072 byte InnoDB limit.

ALTER TABLE SERIES MODIFY COLUMN URL VARCHAR(500) NOT NULL;
ALTER TABLE BOOK MODIFY COLUMN URL VARCHAR(500) NOT NULL;

CREATE UNIQUE INDEX uk__series__library_url ON SERIES (LIBRARY_ID, URL);
CREATE UNIQUE INDEX uk__book__library_url ON BOOK (LIBRARY_ID, URL);
