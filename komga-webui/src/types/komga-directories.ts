export interface SeriesDirectoryBreadcrumbDto {
  name: string,
  path: string,
}

export interface SeriesDirectoryDto {
  name: string,
  path: string,
  parentPath: string,
  directBooksCount: number,
  descendantBooksCount: number,
  childDirectoryCount: number,
  thumbnailBookId?: string,
}

export interface SeriesDirectoryListingDto {
  currentPath: string,
  breadcrumbs: SeriesDirectoryBreadcrumbDto[],
  directories: SeriesDirectoryDto[],
}
