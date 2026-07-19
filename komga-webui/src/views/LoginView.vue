<template>
  <main class="login-page">
    <section class="login-shell" aria-labelledby="login-title">
      <aside class="brand-panel" aria-label="神殇漫画">
        <div class="brand-visual" role="img" aria-label="漫画书库">
          <span class="brand-visual__halo brand-visual__halo--outer" aria-hidden="true"></span>
          <span class="brand-visual__halo brand-visual__halo--inner" aria-hidden="true"></span>
          <div class="brand-visual__mark">
            <v-icon color="white" size="72">mdi-book-open-page-variant</v-icon>
          </div>
          <span class="brand-visual__spark brand-visual__spark--cyan" aria-hidden="true"></span>
          <span class="brand-visual__spark brand-visual__spark--purple" aria-hidden="true"></span>
        </div>
      </aside>

      <section class="login-workspace">
        <header class="login-header">
          <p class="login-header__index">账号登录</p>
          <h2 id="login-title">{{ $t('login.login') }}</h2>
          <p class="login-header__hint">私人漫画与电子书库</p>
        </header>

        <form novalidate :aria-label="$t('login.login')" @submit.prevent="performLogin">
          <div v-if="unclaimed" class="claim-notice">
            <v-alert type="info"
                     icon="mdi-account-plus"
                     prominent
                     text
                     v-html="$t('login.unclaimed_html')"
            >
            </v-alert>
          </div>

          <div v-if="!hideLogin" class="credentials-panel">
            <div class="field-group">
              <v-text-field v-model="form.login"
                            :label="$t('common.email')"
                            :error-messages="getErrors('login')"
                            :autocomplete="hideLogin ? '' : 'username'"
                            autofocus
                            @blur="$v.form.login.$touch()"
              />
            </div>

            <div class="field-group">
              <v-text-field v-model="form.password"
                            :label="$t('common.password')"
                            :error-messages="getErrors('password')"
                            type="password"
                            :autocomplete="hideLogin ? '' : 'current-password'"
                            @input="$v.form.password.$touch()"
                            @blur="$v.form.password.$touch()"
              />
            </div>

            <div class="login-options">
              <v-checkbox v-model="rememberMe"
                          :label="$t('common.remember-me')"
                          hide-details
                          class="mt-0"
              />
            </div>

            <div class="login-actions">
              <v-btn color="primary"
                     type="submit"
                     :disabled="unclaimed"
                     large
                     class="login-primary"
              >{{ $t('login.login') }}
              </v-btn>
              <v-btn v-if="unclaimed"
                     color="primary"
                     large
                     outlined
                     @click="claim"
              >{{ $t('login.create_user_account') }}
              </v-btn>
            </div>
          </div>

          <div v-if="oauth2Providers.length" class="oauth-panel">
            <div class="section-rule"><span>第三方登录</span></div>
            <div class="oauth-list">
              <div
            v-for="provider in oauth2Providers"
            :key="provider.registrationId"
          >
            <v-btn
              :disabled="unclaimed"
              @click="oauth2Login(provider)"
              block
              large
              outlined
              :class="$_.get(socialButtons[provider.registrationId.toLowerCase()], 'text') ? `${socialButtons[provider.registrationId.toLowerCase()].text}--text` : undefined"
              :color="$_.get(socialButtons[provider.registrationId.toLowerCase()], 'color')"
            >
              <v-icon left>mdi-{{ provider.registrationId }}</v-icon>
              使用 {{ provider.name }} 登录
            </v-btn>
              </div>
            </div>
          </div>

          <footer class="login-preferences">
            <div>
            <v-select v-model="locale"
                      :items="locales"
                      :label="$t('home.translation')"
                      prepend-icon="mdi-translate"
            >
            </v-select>
            </div>

            <div>
            <v-select v-model="theme"
                      :items="themes"
                      :label="$t('home.theme')"
                      :prepend-icon="themeIcon"
            >
            </v-select>
            </div>
          </footer>
        </form>
      </section>
    </section>

    <v-snackbar
      v-model="snackbar"
      bottom
      color="error"
    >
      {{ snackText }}
      <v-btn
        text
        @click="snackbar = false"
      >{{ $t('common.close') }}
      </v-btn>
    </v-snackbar>
  </main>
</template>

<script lang="ts">
import Vue from 'vue'
import {email, required} from 'vuelidate/lib/validators'
import {Theme} from '@/types/themes'
import {OAuth2ClientDto} from '@/types/komga-oauth2'
import urls from '@/functions/urls'
import {socialButtons} from '@/types/social'
import {convertErrorCodes} from '@/functions/error-codes'
import {CLIENT_SETTING} from '@/types/komga-clientsettings'

export default Vue.extend({
  name: 'LoginView',
  data: function () {
    return {
      urls,
      socialButtons,
      form: {
        login: '',
        password: '',
      },
      snackbar: false,
      snackText: '',
      unclaimed: false,
      oauth2Providers: [] as OAuth2ClientDto[],
      locales: this.$i18n.availableLocales.map((x: any) => ({text: this.$i18n.t('common.locale_name', x), value: x})),
      clientSettings: {} as Record<string, ClientSettingDto>,
    }
  },
  validations: {
    form: {
      login: {required, email},
      password: {required},
    },
  },
  computed: {
    hideLogin(): boolean {
      return !this.unclaimed
        && this.oauth2Providers.length > 0
        && (this.clientSettings[CLIENT_SETTING.WEBUI_OAUTH2_HIDE_LOGIN]?.value === 'true')
    },
    autoOauth2Login(): boolean {
      return !this.unclaimed
        && this.oauth2Providers.length == 1
        && (this.clientSettings[CLIENT_SETTING.WEBUI_OAUTH2_AUTO_LOGIN]?.value === 'true')
        && !this.$route.query.error
        && !this.$route.query.logout
    },
    locale: {
      get: function (): string {
        return this.$i18n.locale
      },
      set: function (locale: string): void {
        if (this.$i18n.availableLocales.includes(locale)) {
          this.$store.commit('setLocale', locale)
        }
      },
    },

    rememberMe: {
      get: function (): boolean {
        return this.$store.state.persistedState.rememberMe
      },
      set: function (value: boolean): void {
        this.$store.commit('setRememberMe', value)
      },
    },

    themes(): object[] {
      return [
        {text: this.$i18n.t(Theme.LIGHT), value: Theme.LIGHT},
        {text: this.$i18n.t(Theme.DARK), value: Theme.DARK},
        {text: this.$i18n.t(Theme.SYSTEM), value: Theme.SYSTEM},
      ]
    },
    themeIcon(): string {
      switch (this.theme) {
        case Theme.LIGHT:
          return 'mdi-brightness-7'
        case Theme.DARK:
          return 'mdi-brightness-3'
        case Theme.SYSTEM:
          return 'mdi-brightness-auto'
      }
      return ''
    },

    theme: {
      get: function (): Theme {
        return this.$store.state.persistedState.theme
      },
      set: function (theme: Theme): void {
        if (Object.values(Theme).includes(theme)) {
          this.$store.commit('setTheme', theme)
        }
      },
    },
  },
  async mounted() {
    this.getClaimStatus()
    this.clientSettings = await this.$komgaSettings.getClientSettingsGlobal()
    this.oauth2Providers = await this.$komgaOauth2.getProviders()
    if (this.$route.query.error) this.showSnack(convertErrorCodes(this.$route.query.error.toString()))
    if (this.hideLogin && this.autoOauth2Login) this.oauth2Login(this.oauth2Providers[0])
  },
  methods: {
    oauth2Login(provider: OAuth2ClientDto) {
      const url = `${urls.originNoSlash}/oauth2/authorization/${provider.registrationId}`
      const height = 600
      const width = 600
      const y = window.top!.outerHeight / 2 + window.top!.screenY - (height / 2)
      const x = window.top!.outerWidth / 2 + window.top!.screenX - (width / 2)
      window.open(url, 'oauth2Login',
        `toolbar=no,
        location=off,
        status=no,
        menubar=no,
        scrollbars=yes,
        resizable=yes,
        top=${y},
        left=${x},
        width=${height},
        height=${width}`,
      )
    },
    getErrors(fieldName: string): string[] {
      const errors = [] as string[]

      const field = this.$v.form!![fieldName] as any
      if (field && field.$invalid && field.$dirty) {
        if (!field.required) errors.push(this.$t('common.required').toString())
        if (!field.email) errors.push(this.$t('dialog.add_user.field_email_error').toString())
      }
      return errors
    },
    async getClaimStatus() {
      this.unclaimed = !(await this.$komgaClaim.getClaimStatus()).isClaimed
    },
    async performLogin() {
      if (this.isUserValid()) {
        try {
          await this.$store.dispatch(
            'getMeWithAuth',
            {
              login: this.form.login,
              password: this.form.password,
              rememberMe: this.rememberMe,
            })

          await this.$store.dispatch('getLibraries')
          await this.$store.dispatch('getClientSettingsGlobal')
          await this.$store.dispatch('getClientSettingsUser')

          if (this.$route.query.redirect) {
            await this.$router.push({path: this.$route.query.redirect.toString()})
          } else {
            await this.$router.push({name: 'home'})
          }
        } catch (e) {
          this.showSnack(e?.message)
        }
      }
    },
    showSnack(message: string) {
      this.snackText = message
      this.snackbar = true
    },
    isUserValid(): boolean {
      this.$v.$touch()
      return !this.$v.$invalid
    },
    async claim() {
      if (this.isUserValid()) {
        try {
          await this.$komgaClaim.claimServer({
            email: this.form.login,
            password: this.form.password,
          } as ClaimAdmin)

          await this.performLogin()
        } catch (e) {
          this.showSnack(e.message)
        }
      }
    },
  },
})
</script>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: clamp(1rem, 4vw, 4rem);
  background:
    radial-gradient(circle at 14% 12%, rgb(91 223 255 / 16%), transparent 34rem),
    radial-gradient(circle at 88% 82%, rgb(162 130 255 / 15%), transparent 36rem),
    var(--k-surface-page);
}

.login-shell {
  display: grid;
  grid-template-columns: minmax(20rem, 0.88fr) minmax(26rem, 1.12fr);
  width: min(100%, 74rem);
  min-height: min(45rem, calc(100vh - 4rem));
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--k-primary) 22%, transparent);
  border-radius: 28px;
  background: var(--k-surface-card);
  box-shadow: 0 32px 90px rgb(2 8 24 / 42%), 0 0 70px color-mix(in srgb, var(--k-primary) 9%, transparent);
}

.brand-panel {
  position: relative;
  display: grid;
  place-items: center;
  overflow: hidden;
  min-height: 34rem;
  padding: clamp(2rem, 4vw, 4rem);
  color: #f8fafc;
  background:
    linear-gradient(145deg, rgb(9 28 55 / 92%), rgb(18 24 65 / 96%)),
    #071329;
}

.brand-panel::before,
.brand-panel::after {
  position: absolute;
  content: "";
  filter: blur(8px);
}

.brand-panel::before {
  top: -14rem;
  left: -12rem;
  width: 28rem;
  height: 28rem;
  border-radius: 50%;
  background: rgb(92 225 255 / 13%);
}

.brand-panel::after {
  right: -13rem;
  bottom: -15rem;
  width: 32rem;
  height: 32rem;
  border-radius: 50%;
  background: rgb(157 132 255 / 14%);
}

.brand-visual {
  position: relative;
  display: grid;
  width: min(24rem, 78%);
  aspect-ratio: 1;
  place-items: center;
}

.brand-visual__halo {
  position: absolute;
  border: 1px solid rgb(122 224 255 / 22%);
  border-radius: 50%;
}

.brand-visual__halo--outer {
  inset: 0;
  box-shadow: inset 0 0 5rem rgb(85 209 255 / 6%), 0 0 5rem rgb(105 95 255 / 12%);
}

.brand-visual__halo--inner {
  inset: 17%;
  border-color: rgb(179 151 255 / 28%);
  transform: rotate(-14deg);
}

.brand-visual__mark {
  position: relative;
  z-index: 1;
  display: grid;
  width: 8.5rem;
  height: 8.5rem;
  place-items: center;
  border: 1px solid rgb(255 255 255 / 30%);
  border-radius: 2.25rem;
  background: linear-gradient(145deg, #58d9ff, #826dff 56%, #e873b0);
  box-shadow: 0 1.5rem 4rem rgb(46 144 255 / 28%), inset 0 1px rgb(255 255 255 / 38%);
  transform: rotate(-5deg);
}

.brand-visual__spark {
  position: absolute;
  width: .75rem;
  height: .75rem;
  border-radius: 50%;
  box-shadow: 0 0 1.5rem currentColor;
}

.brand-visual__spark--cyan {
  top: 19%;
  right: 14%;
  background: #66e0ff;
  color: #66e0ff;
}

.brand-visual__spark--purple {
  bottom: 18%;
  left: 13%;
  background: #b49aff;
  color: #b49aff;
}

.login-header__index {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.login-workspace {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: clamp(2rem, 6vw, 5.5rem);
  background: linear-gradient(155deg, color-mix(in srgb, var(--k-surface-card) 96%, var(--k-primary) 4%), var(--k-surface-card));
}

.login-header {
  margin-bottom: 2rem;
}

.login-header__index {
  margin: 0 0 0.75rem;
  color: var(--k-primary);
}

.login-header h2 {
  margin: 0;
  color: var(--k-text-primary);
  font-family: Inter, "Noto Sans SC", system-ui, sans-serif;
  font-size: clamp(2rem, 4vw, 3.25rem);
  line-height: 1.1;
}

.login-header__hint {
  margin: 0.75rem 0 0;
  color: var(--k-text-secondary);
}

.claim-notice,
.field-group {
  margin-bottom: 0.5rem;
}

.login-options {
  margin-top: -0.5rem;
}

.login-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1.25rem;
}

.login-primary {
  min-width: 9rem;
  border-radius: 14px;
  box-shadow: 0 12px 28px color-mix(in srgb, var(--k-primary) 28%, transparent);
}

.oauth-panel {
  margin-top: 1.75rem;
}

.section-rule {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
  color: var(--k-text-secondary);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 0.6875rem;
  letter-spacing: 0.12em;
}

.section-rule::after {
  flex: 1;
  height: 1px;
  background: var(--k-border);
  content: "";
}

.oauth-list {
  display: grid;
  gap: 0.75rem;
}

.login-preferences {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 2.25rem;
  padding-top: 1rem;
  border-top: 1px solid var(--k-border);
}

@media (max-width: 839px) {
  .login-page {
    padding: 0;
    place-items: stretch;
  }

  .login-shell {
    display: block;
    min-height: 100vh;
    border: 0;
    box-shadow: none;
  }

  .brand-panel {
    min-height: 12rem;
    padding: 1.5rem;
  }

  .brand-visual {
    width: 9rem;
  }

  .brand-visual__mark {
    width: 5rem;
    height: 5rem;
    border-radius: 1.4rem;
  }

  .brand-visual__mark .v-icon {
    font-size: 2.75rem !important;
  }

  .login-workspace {
    padding: 2.25rem 1.5rem 3rem;
  }
}

@media (max-width: 479px) {
  .login-actions,
  .login-preferences {
    grid-template-columns: 1fr;
  }

  .login-actions {
    display: grid;
  }

  .login-actions .v-btn {
    width: 100%;
  }

  .login-preferences {
    gap: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-page *,
  .login-page *::before,
  .login-page *::after {
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
  }
}

</style>
