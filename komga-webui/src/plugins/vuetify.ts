import '@mdi/font/css/materialdesignicons.css'
import Vue from 'vue'
import Vuetify from 'vuetify/lib'

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
        base: '#FFFFFF',
        primary: '#245FCC',
        secondary: '#087F78',
        accent: '#D9574E',
        success: '#087F78',
        warning: '#A65D00',
        error: '#C62828',
        info: '#245FCC',
        'contrast-1': '#EEF2F6',
        'contrast-light-2': '#5A6878',
        'diff': '#D7F0ED',
      },
      dark: {
        base: '#1A212B',
        primary: '#79A7FF',
        secondary: '#62CEC5',
        accent: '#FF8B80',
        success: '#62CEC5',
        warning: '#F2B55F',
        error: '#FF8585',
        info: '#79A7FF',
        'contrast-1': '#242E3A',
        'contrast-light-2': '#B5C0CC',
        'diff': '#203D3B',
      },
    },
  },
})
