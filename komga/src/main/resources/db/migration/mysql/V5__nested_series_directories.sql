alter table `LIBRARY`
    add column `SERIES_GROUPING_MODE` varchar(32) NOT NULL DEFAULT 'DIRECT_PARENT';

alter table `BOOK`
    add column `DIRECTORY_PATH` varchar(2048) NOT NULL DEFAULT '';

create index `IDX_BOOK_SERIES_DIRECTORY_PATH` on `BOOK` (`SERIES_ID`, `DIRECTORY_PATH`(512));
