// vue.config.js
module.exports = {
  // with './' the dev server cannot load any arbitrary path
  // with '/' the prod build generates some url(/fonts…) calls in the css chunks, which doesn't work with a servlet context path
  publicPath: '/',

  pluginOptions: {
    i18n: {
      locale: 'en',
      fallbackLocale: 'en',
      localeDir: 'locales',
      enableInSFC: false,
    },
  },

  devServer: {
    allowedHosts: 'all',
    client: {
      webSocketURL: 'ws://0.0.0.0:8081/ws',
    },
  },

  // The Vue 2 codebase is large enough that the production TypeScript checker
  // can exceed Vue CLI's default child-process heap on current Node releases.
  chainWebpack: config => {
    if (process.env.SKIP_TYPECHECK === 'true') {
      config.plugins.delete('fork-ts-checker')
      return
    }
    config.plugin('fork-ts-checker').tap(args => {
      args[0].typescript = {
        ...(args[0].typescript || {}),
        memoryLimit: 6144,
      }
      return args
    })
  },

  // custom rule for readium and r2d2bc css that needs to be made available, but untouched
  configureWebpack: {
    module: {
      rules: [
        {
          test: [
            /readium\/.*\.css.resource$/,
            /r2d2bc\/.*\.css.resource$/,
          ],
          type: 'asset/resource',
          generator: {
            filename: 'css/[hash].css[query]',
          },
        },
      ],
    },
  },
}
