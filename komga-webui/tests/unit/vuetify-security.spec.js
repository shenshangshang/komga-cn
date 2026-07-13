const fs = require('fs')

function loadMethod(file, signature, dependencies = {}) {
  const source = fs.readFileSync(require.resolve(file), 'utf8')
  const start = source.indexOf(signature)
  if (start < 0) throw new Error(`Missing ${signature}`)
  const bodyStart = source.indexOf('{', start + signature.length)
  let depth = 0
  let end = bodyStart
  for (; end < source.length; end += 1) {
    if (source[end] === '{') depth += 1
    if (source[end] === '}' && --depth === 0) break
  }
  const args = Object.keys(dependencies)
  const values = Object.values(dependencies)
  return Function(...args, `return function ${signature}${source.slice(bodyStart, end + 1)}`)(...values)
}

describe('Vuetify 2.7.1 security backports', () => {
  it('does not merge inherited or prototype-mutating keys', () => {
    const isObject = obj => obj !== null && typeof obj === 'object'
    const mergeDeep = loadMethod('vuetify/lib/util/helpers.js', 'mergeDeep(source = {}, target = {})', {
      isObject,
    })
    const inherited = { inherited: 'unsafe' }
    const payload = Object.create(inherited)
    Object.defineProperty(payload, '__proto__', {
      enumerable: true,
      value: { polluted: true },
    })
    payload.constructor = { prototype: { polluted: true } }
    payload.prototype = { polluted: true }
    payload.safe = 'kept'

    const result = mergeDeep({}, payload)

    expect(result).toEqual({ safe: 'kept' })
    expect(Object.prototype.polluted).toBeUndefined()
  })

  it('renders malicious date formatter output as text, never innerHTML', () => {
    const genTitleText = loadMethod(
      'vuetify/lib/components/VDatePicker/VDatePickerTitle.js',
      'genTitleText()',
    )
    const malicious = '<img src=x onerror=alert(1)>'
    const h = jest.fn((tag, data, children) => ({ tag, data, children }))
    const vm = {
      $createElement: h,
      computedTransition: 'picker-transition',
      date: malicious,
      value: '2026-07-13',
    }

    const vnode = genTitleText.call(vm)
    const dateNode = vnode.children[0]

    expect(dateNode.data.domProps).toBeUndefined()
    expect(dateNode.children).toEqual([malicious])
  })
})
