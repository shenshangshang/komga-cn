import fs from 'fs'
import path from 'path'

describe('History readable labels', () => {
  const source = fs.readFileSync(path.resolve(__dirname, '../../../src/views/HistoryView.vue'), 'utf8')

  it('renders the localized event action as text, not only an icon', () => {
    expect(source).toContain('{{ $t(`enums.historical_event_type.${item.type}`) }}')
  })

  it('never falls back to opaque database ids for deleted books or series', () => {
    const bookStart = source.lastIndexOf('displayBookName(item:')
    const seriesStart = source.lastIndexOf('displaySeriesName(item:')
    const bookMethod = source.slice(bookStart, source.indexOf('\n    },', bookStart))
    const seriesMethod = source.slice(seriesStart, source.indexOf('\n    },', seriesStart))

    expect(bookMethod).not.toContain('item.bookId')
    expect(seriesMethod).not.toContain('item.seriesId')
    expect(bookMethod).toContain('this.$t(\'history.deleted_book\')')
    expect(seriesMethod).toContain('this.$t(\'history.deleted_series\')')
  })
})
