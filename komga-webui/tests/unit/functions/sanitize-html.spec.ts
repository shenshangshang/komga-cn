import {sanitizeRichHtml} from '@/functions/sanitize-html'

describe('sanitizeRichHtml', () => {
  it('removes executable markup while preserving safe rich text', () => {
    const dirty = '<p onclick="alert(1)">safe <strong>text</strong></p><script>alert(1)</script><img src="x" onerror="alert(1)">'
    const clean = sanitizeRichHtml(dirty)
    expect(clean).toContain('<p>safe <strong>text</strong></p>')
    expect(clean).not.toMatch(/script|onclick|onerror/)
  })

  it('rejects executable URL schemes and hardens links', () => {
    const clean = sanitizeRichHtml('<a href="javascript:alert(1)" target="_blank">bad</a><a href="https://example.com">good</a>')
    expect(clean).not.toContain('javascript:')
    expect(clean).toContain('href="https://example.com"')
    expect(clean).toContain('rel="noopener noreferrer"')
  })
})
