const { execFileSync } = require('child_process')

describe('Vue 2 parseHTML ReDoS backport', () => {
  it('compiles an adversarial malformed raw-text template within a bounded time', () => {
    const script = `
      global.document = {
        createElement: () => ({
          textContent: '',
          set innerHTML(value) { this.textContent = value },
        }),
      }
      const Vue = require('./node_modules/vue/dist/vue.common.js')
      Vue.config.warnHandler = () => {}
      const template = '<div><script>' + '<'.repeat(100000) + '</textarea></div>'
      Vue.compile(template)
      require('vue-template-compiler').compile(template)
    `

    expect(() => execFileSync(process.execPath, ['-e', script], {
      cwd: process.cwd(),
      stdio: 'pipe',
      timeout: 1000,
    })).not.toThrow()
  })

  it('preserves valid raw-text element parsing', () => {
    const Vue = require('vue/dist/vue.common.js')
    const script = Vue.compile('<div><textarea>x < y</textarea></div>')

    const generated = [script.render, ...script.staticRenderFns].join('\n')
    expect(generated).toContain('x < y')
  })
})
