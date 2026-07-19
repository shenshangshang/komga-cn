export interface SeriesBrowseIdentity {
  title: string,
  booksCount: number,
}

export function getSeriesBrowseIdentity(
  seriesTitle: string,
  seriesBooksCount: number,
  currentDirectoryPath: string,
  directoryBooksCount: number,
): SeriesBrowseIdentity {
  const directorySegments = currentDirectoryPath.split('/').filter(Boolean)

  if (directorySegments.length === 0) {
    return {
      title: seriesTitle,
      booksCount: seriesBooksCount,
    }
  }

  return {
    title: directorySegments[directorySegments.length - 1],
    booksCount: directoryBooksCount,
  }
}
