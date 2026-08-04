export function addLibraryCreationRoot(roots: string[], candidate: string): string[] {
  const normalized = candidate.trim()
  if (!normalized || roots.includes(normalized)) return roots
  return [...roots, normalized]
}
