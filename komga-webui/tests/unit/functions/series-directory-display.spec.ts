import {getSeriesBrowseIdentity} from '@/functions/series-directory-display'

describe('getSeriesBrowseIdentity', () => {
  it('keeps the series title and count at the series root', () => {
    expect(getSeriesBrowseIdentity('蛇之樱', 11, '', 0)).toEqual({
      title: '蛇之樱',
      booksCount: 11,
    })
  })

  it('uses the current directory name and its descendant book count', () => {
    expect(getSeriesBrowseIdentity('蛇之樱', 11, '终章/蛇之僕·櫻 終（上）', 3)).toEqual({
      title: '蛇之僕·櫻 終（上）',
      booksCount: 3,
    })
  })
})
