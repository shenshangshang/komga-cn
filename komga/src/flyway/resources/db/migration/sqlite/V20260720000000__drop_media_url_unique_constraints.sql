-- Drop the UNIQUE (LIBRARY_ID, URL) constraints on BOOK/SERIES (see the MySQL migration of the
-- same name). Kept in sync so jOOQ generation matches the runtime schema.
DROP INDEX IF EXISTS uk__book__library_url;
DROP INDEX IF EXISTS uk__series__library_url;
