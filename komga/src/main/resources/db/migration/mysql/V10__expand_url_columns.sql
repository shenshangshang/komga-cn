-- Chinese directory names are percent-encoded in URLs (each char → 9 bytes
-- like %E6%BC%AB), so VARCHAR(500) overflows on deeply nested paths, causing
-- BatchUpdateException on scan. Expand the columns and use a prefix index
-- to stay within MySQL's 3072-byte index limit under utf8mb4.

ALTER TABLE BOOK DROP INDEX uk__book__library_url;
ALTER TABLE BOOK MODIFY COLUMN URL VARCHAR(1500) NOT NULL;
CREATE UNIQUE INDEX uk__book__library_url ON BOOK (LIBRARY_ID, URL(500));

ALTER TABLE SERIES DROP INDEX uk__series__library_url;
ALTER TABLE SERIES MODIFY COLUMN URL VARCHAR(1500) NOT NULL;
CREATE UNIQUE INDEX uk__series__library_url ON SERIES (LIBRARY_ID, URL(500));
