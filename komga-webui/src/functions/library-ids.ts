import {LIBRARIES_ALL} from '@/types/library'

interface LibraryIdRef {
  id: string,
}

export function getLibraryIdsForAggregation(
  libraryId: string,
  libraries: LibraryIdRef[],
  selectedLibraryIds: string[] = [],
): string[] {
  if (libraryId !== LIBRARIES_ALL) return [libraryId]
  if (selectedLibraryIds.length > 0) return [...selectedLibraryIds]
  return libraries.map(library => library.id)
}
