import fs from 'fs'
import path from 'path'

describe('ReusableDialogs book deletion', () => {
  it('publishes a local BookDeleted event after the synchronous delete finishes', () => {
    const source = fs.readFileSync(path.resolve(__dirname, '../../../src/components/ReusableDialogs.vue'), 'utf8')
    const deleteMethod = source.slice(source.indexOf('async deleteBooks()'), source.indexOf('\n    },', source.indexOf('async deleteBooks()')))

    expect(deleteMethod).toMatch(/await this\.\$komgaBooks\.deleteBook\(b\.id\)[\s\S]*this\.\$eventHub\.\$emit\(BOOK_DELETED, \{[\s\S]*bookId: b\.id,[\s\S]*seriesId: b\.seriesId,[\s\S]*libraryId: b\.libraryId,/)
  })
})
