import fs from 'fs'
import path from 'path'
import {sanitizeRichHtml} from '@/functions/sanitize-html'

describe('rich HTML component sanitization', () => {
  it.each([
    ['item cards', '../../../src/components/ItemCard.vue'],
    ['confirmation dialogs', '../../../src/components/dialogs/ConfirmationDialog.vue'],
  ])('exposes the shared sanitizer to %s templates', (_, relativePath) => {
    const source = fs.readFileSync(path.resolve(__dirname, relativePath), 'utf8')

    expect(source.match(/^  data:\s*\(\)\s*=>/gm)).toHaveLength(1)
    expect(source).toMatch(/^      sanitizeRichHtml,$/m)
  })

  it('keeps executable markup out of rendered component HTML', () => {
    expect(sanitizeRichHtml('<script>alert(1)</script><strong>safe</strong>')).toBe('<strong>safe</strong>')
  })
})
