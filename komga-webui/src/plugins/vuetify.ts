import '@mdi/font/css/materialdesignicons.css'
import 'typeface-roboto/index.css'
import Vue from 'vue'
import Vuetify from 'vuetify/lib'
import colors from 'vuetify/lib/util/colors'

import {Touch} from 'vuetify/lib/directives'
import i18n from '@/i18n'
import IconFormatLineSpacingDown from '@/components/icons/IconFormatLineSpacingDown.vue'

Vue.use(Vuetify, {
  directives: {
    Touch,
  },
})

export default new Vuetify({
  icons: {
    iconfont: 'mdi',
    values: {
      formatLineSpacingDown: {
        component: IconFormatLineSpacingDown,
      },
    },
  },

  lang: {
    t: (key, ...params) => i18n.t(key, params).toString(),
  },

  theme: {
    options: {
      customProperties: true,
    },
    themes: {
      light: {
        base: '#ffffff',
        primary: '#1565C0',
        secondary: '#FFB300',
        accent: '#E53935',
        'contrast-1': '#F5F7FA',
        'contrast-light-2': colors.grey.darken2,
        'diff': colors.green.lighten4,
      },
      dark: {
        base: '#121212',
        primary: '#64B5F6',
        secondary: '#FFB300',
        accent: '#E53935',
        'contrast-1': '#1E1E1E',
        'contrast-light-2': colors.grey.lighten2,
        'diff': colors.green.darken4,
      },
    },
  },
})
