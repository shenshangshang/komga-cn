import {addLibraryCreationRoot} from '@/functions/library-creation-roots'

describe('addLibraryCreationRoot', () => {
  it('trims and appends a new root without mutating the original list', () => {
    const roots = ['/data/shared']

    expect(addLibraryCreationRoot(roots, '  /data/users  ')).toEqual(['/data/shared', '/data/users'])
    expect(roots).toEqual(['/data/shared'])
  })

  it('ignores blank and duplicate roots', () => {
    const roots = ['/data/shared']

    expect(addLibraryCreationRoot(roots, '  ')).toBe(roots)
    expect(addLibraryCreationRoot(roots, '/data/shared')).toBe(roots)
  })
})
