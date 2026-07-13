import sanitizeHtml from 'sanitize-html'

export function sanitizeRichHtml(value: string): string {
  return sanitizeHtml(value, {
    allowedTags: ['a', 'b', 'blockquote', 'br', 'code', 'em', 'h1', 'h2', 'h3', 'i', 'img', 'li', 'ol', 'p', 'pre', 'span', 'strong', 'ul'],
    allowedAttributes: {a: ['href', 'target', 'rel'], img: ['src', 'alt', 'title'], '*': ['class']},
    allowedSchemes: ['http', 'https', 'mailto'],
    transformTags: {a: sanitizeHtml.simpleTransform('a', {rel: 'noopener noreferrer'})},
  })
}
