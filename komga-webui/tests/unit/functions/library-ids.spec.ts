import {getLibraryIdsForAggregation} from '@/functions/library-ids'

describe('getLibraryIdsForAggregation', () => {
  const libraries = [
    {id: 'pinned'},
    {id: 'unpinned'},
  ]

  it('includes every library in the all-libraries aggregate', () => {
    expect(getLibraryIdsForAggregation('all', libraries)).toEqual(['pinned', 'unpinned'])
  })

  it('keeps an individual library selection scoped', () => {
    expect(getLibraryIdsForAggregation('pinned', libraries)).toEqual(['pinned'])
  })

  it('respects an explicit dashboard selection', () => {
    expect(getLibraryIdsForAggregation('all', libraries, ['unpinned'])).toEqual(['unpinned'])
  })
})
