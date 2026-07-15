alter table library
    add column SERIES_GROUPING_MODE text NOT NULL DEFAULT 'DIRECT_PARENT';

alter table book
    add column DIRECTORY_PATH text NOT NULL DEFAULT '';

create index IDX_BOOK_SERIES_DIRECTORY_PATH on BOOK (SERIES_ID, DIRECTORY_PATH);
