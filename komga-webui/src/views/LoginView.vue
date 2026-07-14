<template>
  <main class="login-page">
    <section class="login-shell" aria-labelledby="login-title">
      <aside class="brand-panel" aria-label="Komga">
        <div class="brand-panel__top">
          <v-img src="../assets/logo.svg"
                 alt="Komga"
                 contain
                 class="brand-logo"
                 max-width="156"
          />
          <span class="brand-edition">INKFRAME / LIBRARY</span>
        </div>

        <div class="brand-statement">
          <p class="brand-kicker">YOUR STORIES, IN ORDER</p>
          <h1>每一格，<br><span>都在等你继续。</span></h1>
          <p class="brand-copy">收藏、整理并继续阅读你的漫画世界。</p>
        </div>

        <div class="spine-index" aria-hidden="true">
          <span class="spine-index__line"></span>
          <span>01</span>
          <span>LIBRARY</span>
          <span class="spine-index__page">KMG—CN</span>
        </div>
      </aside>

      <section class="login-workspace">
        <header class="login-header">
          <p class="login-header__index">ACCESS / 01</p>
          <h2 id="login-title">{{ $t('login.login') }}</h2>
          <p class="login-header__hint">Komga personal media library</p>
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
            <div class="section-rule"><span>SSO</span></div>
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
              Sign in with {{ provider.name }}
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
  background: var(--k-surface-page);
}

.login-shell {
  display: grid;
  grid-template-columns: minmax(20rem, 0.88fr) minmax(26rem, 1.12fr);
  width: min(100%, 74rem);
  min-height: min(45rem, calc(100vh - 4rem));
  overflow: hidden;
  border: 2px solid var(--k-text-primary);
  background: var(--k-surface-card);
  box-shadow: 12px 12px 0 color-mix(in srgb, var(--k-primary) 24%, transparent);
}

.brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  padding: clamp(2rem, 4vw, 4rem);
  color: #f8fafc;
  background: #111318;
}

.brand-panel::after {
  position: absolute;
  right: 9%;
  bottom: -10%;
  width: 48%;
  height: 58%;
  border: 1px solid rgb(255 255 255 / 18%);
  border-bottom: 0;
  content: "";
  transform: skewY(-9deg);
}

.brand-panel__top,
.spine-index {
  position: relative;
  z-index: 1;
}

.brand-panel__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.brand-logo {
  flex: 0 1 9.75rem;
  filter: brightness(0) invert(1);
}

.brand-edition,
.brand-kicker,
.login-header__index,
.spine-index {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.brand-edition {
  text-align: right;
  color: rgb(255 255 255 / 60%);
}

.brand-statement {
  position: relative;
  z-index: 1;
  max-width: 28rem;
  margin: 4rem 0;
}

.brand-kicker {
  margin-bottom: 1.5rem;
  color: #ff7568;
}

.brand-statement h1 {
  margin: 0;
  font-family: "Noto Serif SC", "Source Han Serif SC", "Songti SC", serif;
  font-size: clamp(2.5rem, 5vw, 4.75rem);
  font-weight: 700;
  line-height: 1.05;
  letter-spacing: -0.06em;
}

.brand-statement h1 span {
  color: #8190ff;
}

.brand-copy {
  max-width: 22rem;
  margin: 2rem 0 0;
  color: rgb(255 255 255 / 68%);
  font-size: 1rem;
  line-height: 1.7;
}

.spine-index {
  display: grid;
  grid-template-columns: auto 2rem auto 1fr;
  align-items: center;
  gap: 0.75rem;
  color: rgb(255 255 255 / 58%);
}

.spine-index__line {
  width: 0.375rem;
  height: 3rem;
  background: #ef4938;
}

.spine-index__page {
  justify-self: end;
}

.login-workspace {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: clamp(2rem, 6vw, 5.5rem);
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
  font-family: "Noto Serif SC", "Source Han Serif SC", "Songti SC", serif;
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
    min-height: 17rem;
    padding: 1.5rem;
  }

  .brand-statement {
    margin: 2.5rem 0 1.5rem;
  }

  .brand-statement h1 {
    font-size: clamp(2.25rem, 11vw, 3.5rem);
  }

  .brand-copy,
  .brand-panel::after {
    display: none;
  }

  .spine-index {
    grid-template-columns: auto 2rem auto 1fr;
  }

  .spine-index__line {
    height: 1.75rem;
  }

  .login-workspace {
    padding: 2.25rem 1.5rem 3rem;
  }
}

@media (max-width: 479px) {
  .brand-edition,
  .spine-index__page {
    display: none;
  }

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
