<template>
  <div class="app-shell fill-height">
    <v-app-bar
      app
      color="base"
      class="app-shell__bar"
    >
      <v-badge
        dot
        offset-x="15"
        offset-y="20"
        :value="drawerVisible ? 0 : $store.state.booksToCheck + $store.getters.getUnreadAnnouncementsCount()"
        :color="$store.state.booksToCheck ? 'accent' : 'info'"
        :class="{'ms-n3': !isMobile}"
      >
        <v-app-bar-nav-icon
          class="k-touch-target"
          :aria-label="$t(navigationToggleLabel)"
          @click.stop="toggleDrawer"
        />
      </v-badge>

      <router-link
        v-if="!isMobile"
        :to="{name: 'home'}"
        class="app-shell__wordmark link-none"
        aria-label="神殇漫画"
      >
        神殇漫画
      </router-link>

      <search-box class="app-shell__search flex-fill"/>

    </v-app-bar>

    <v-navigation-drawer
      app
      v-model="drawerVisible"
      :right="$vuetify.rtl"
      :permanent="!isMobile"
      :temporary="isMobile"
      :mini-variant="!isMobile && navigationCollapsed"
      :width="300"
      :mini-variant-width="80"
      :mobile-breakpoint="0"
      class="app-shell__drawer"
      :class="{'app-shell__drawer--collapsed': !isMobile && navigationCollapsed}"
      :aria-label="$t('shell.primary_navigation')"
    >
      <v-list-item
        inactive
        class="app-shell__brand pb-2"
        aria-label="神殇漫画"
        @click="$router.push({name: 'home'})"
      >
        <v-list-item-avatar>
          <v-img src="../assets/logo.svg" alt=""/>
        </v-list-item-avatar>

        <v-list-item-content>
          <v-list-item-title class="app-shell__brand-title">
            神殇漫画
          </v-list-item-title>
          <v-list-item-subtitle class="app-shell__brand-subtitle">个人漫画书库</v-list-item-subtitle>
        </v-list-item-content>

        <v-list-item-action v-if="!isMobile" class="ma-0">
          <v-btn
            icon
            class="k-touch-target"
            :aria-label="$t(navigationCollapsed ? 'shell.expand_navigation' : 'shell.collapse_navigation')"
            @click.stop.prevent="navigationCollapsed = !navigationCollapsed"
          >
            <v-icon>{{ collapseIcon }}</v-icon>
          </v-btn>
        </v-list-item-action>

        <v-tooltip left>
          <template v-slot:activator="{ on }">
            <v-progress-linear
              :active="taskCount > 0"
              indeterminate
              absolute
              bottom
              height="5"
              color="secondary"
              v-on="on"
            />
          </template>
          <div class="mb-2">{{ $tc('common.pending_tasks', taskCount) }}</div>
          <div v-for="taskType in Object.keys(taskCountByType)"
               :key="taskType"
          >{{ taskType }}: {{ taskCountByType[taskType] }}
          </div>
        </v-tooltip>
      </v-list-item>

      <v-divider/>

      <v-slide-x-transition hide-on-leave>
        <reorder-libraries v-if="showReorder" @dismiss="showReorder = false"/>
      </v-slide-x-transition>

      <template v-if="!showReorder">
        <v-list nav dense class="app-shell__nav-list">
          <v-list-item :to="{name: 'dashboard'}">
            <v-list-item-icon>
              <v-icon>mdi-home</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>{{ $t('navigation.home') }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>

          <!--   LIBRARIES     -->
          <v-list-item
            :to="{name:'libraries', params: {libraryId: LIBRARIES_ALL}}"
            class="app-shell__library-parent"
            :aria-label="$t('navigation.libraries')"
          >
            <v-tooltip right :disabled="!navigationCollapsed">
              <template v-slot:activator="{ on }">
                <v-list-item-icon v-on="on">
                  <v-icon>mdi-book-multiple</v-icon>
                </v-list-item-icon>
              </template>
              <span>{{ $t('navigation.libraries') }}</span>
            </v-tooltip>
            <v-list-item-content>
              <v-list-item-title>{{ $t('navigation.libraries') }}</v-list-item-title>
            </v-list-item-content>
            <v-list-item-action v-if="$store.getters.meCreateLibrary" class="ma-0">
              <v-btn
                icon
                class="k-touch-target"
                :aria-label="$t('dialog.edit_library.dialog_title_add')"
                @click.stop.capture.prevent="addLibrary"
              >
                <v-icon>mdi-plus</v-icon>
              </v-btn>
            </v-list-item-action>
            <v-list-item-action class="ma-0">
              <libraries-actions-menu @reorder="showReorder = true"/>
            </v-list-item-action>
          </v-list-item>

          <div class="app-shell__library-children" role="group" :aria-label="$t('navigation.libraries')">
            <!--   PINNED LIBRARIES     -->
            <v-list-item v-for="(l, index) in librariesPinned"
                         :key="index"
                         :to="{name:'libraries', params: {libraryId: l.id}}"
                         class="app-shell__library-child"
                         :aria-label="l.name"
            >
              <v-list-item-icon>
                <v-icon small>mdi-bookshelf</v-icon>
              </v-list-item-icon>
              <v-list-item-content>
                <v-list-item-title>{{ l.name }}</v-list-item-title>
                <v-list-item-subtitle
                  v-if="l.unavailable"
                  class="error--text caption"
                >{{ $t('common.unavailable') }}
                </v-list-item-subtitle>
              </v-list-item-content>
              <v-list-item-action class="ma-0" v-if="isAdmin">
                <library-actions-menu :library="l"/>
              </v-list-item-action>
            </v-list-item>

            <!--   UNPINNED LIBRARIES     -->
            <v-list-group no-action
                          sub-group
                          class="app-shell__library-more"
                          v-if="librariesUnpinned.length > 0"
                          v-model="expandUnpinned"
            >
              <template v-slot:activator>
                <v-list-item-title>{{ $t('common.more') }}</v-list-item-title>
              </template>

              <v-list-item v-for="(l, index) in librariesUnpinned"
                           :key="index"
                           :to="{name:'libraries', params: {libraryId: l.id}}"
                           class="app-shell__library-child app-shell__library-child--nested"
                           :aria-label="l.name"
              >
                <v-list-item-icon>
                  <v-icon small>mdi-bookshelf</v-icon>
                </v-list-item-icon>
                <v-list-item-content>
                  <v-list-item-title>{{ l.name }}</v-list-item-title>
                  <v-list-item-subtitle
                    v-if="l.unavailable"
                    class="error--text caption"
                  >{{ $t('common.unavailable') }}
                  </v-list-item-subtitle>
                </v-list-item-content>
                <v-list-item-action class="ma-0">
                  <library-actions-menu :library="l"/>
                </v-list-item-action>
              </v-list-item>
            </v-list-group>
          </div>

          <!--   IMPORT     -->
          <v-list-group v-if="isAdmin || $store.getters.meUploadBook"
                        prepend-icon="mdi-import"
                        no-action
                        v-model="expandImport"
          >
            <template v-slot:activator>
              <v-list-item-title>{{ $t('book_import.title') }}</v-list-item-title>
            </template>

            <v-list-item v-if="isAdmin" :to="{name: 'import-books'}">
              <v-list-item-title>{{ $t('common.books') }}</v-list-item-title>
            </v-list-item>

            <v-list-item v-if="isAdmin" :to="{name: 'import-readlist'}">
              <v-list-item-title>{{ $t('common.readlist') }}</v-list-item-title>
            </v-list-item>
            <v-list-item v-if="$store.getters.meUploadBook" :to="{name: 'upload-books'}">
              <v-list-item-title>上传漫画压缩包</v-list-item-title>
            </v-list-item>
          </v-list-group>

          <!--   MEDIA MANAGEMENT     -->
          <v-list-group v-if="isAdmin"
                        no-action
                        v-model="expandMediaManagement"
          >
            <template v-slot:prependIcon>
              <v-badge
                dot
                inline
                :value="$store.state.booksToCheck"
                color="accent"
              >
                <v-icon>mdi-book-cog</v-icon>
              </v-badge>
            </template>
            <template v-slot:activator>
              <v-list-item-title>{{ $t('common.media') }}</v-list-item-title>
            </template>

            <v-list-item :to="{name: 'media-analysis'}">
              <v-badge
                dot
                inline
                :value="$store.state.booksToCheck"
                color="accent"
              >
                <v-list-item-title>{{ $t('media_analysis.media_analysis') }}</v-list-item-title>
              </v-badge>
            </v-list-item>

            <v-list-item :to="{name: 'missing-posters'}">
              <v-list-item-title>{{ $t('missing_posters.title') }}</v-list-item-title>
            </v-list-item>

            <v-list-item :to="{name: 'duplicate-files'}">
              <v-list-item-title>{{ $t('duplicates.title') }}</v-list-item-title>
            </v-list-item>

            <v-list-group no-action
                          sub-group
                          v-model="expandDuplicatePages"
            >
              <template v-slot:activator>
                <v-list-item-title>{{ $t('duplicate_pages.title') }}</v-list-item-title>
              </template>

              <v-list-item :to="{name: 'settings-duplicate-pages-known'}">
                <v-list-item-title>{{ $t('duplicate_pages.known') }}</v-list-item-title>
              </v-list-item>

              <v-list-item :to="{name: 'settings-duplicate-pages-unknown'}">
                <v-list-item-title>{{ $t('duplicate_pages.new') }}</v-list-item-title>
              </v-list-item>
            </v-list-group>
          </v-list-group>

          <v-list-item :to="{name: 'history'}" v-if="isAdmin">
            <v-list-item-icon>
              <v-icon>mdi-clock-time-four-outline</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>{{ $t('history.title') }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>

          <v-list-item :to="{name: 'reading-stats'}">
            <v-list-item-icon>
              <v-icon>mdi-chart-bar</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>{{ $t('reading_stats.title') }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>

          <!--   SETTINGS     -->
          <v-list-group v-if="isAdmin"
                        no-action
                        v-model="expandSettings"
          >
            <template v-slot:prependIcon>
              <v-badge
                dot
                inline
                :value="$store.getters.getUnreadAnnouncementsCount()"
                color="info"
              >
                <v-icon>mdi-cog</v-icon>
              </v-badge>
            </template>
            <template v-slot:activator>
              <v-list-item-title>{{ $t('server.tab_title') }}</v-list-item-title>
            </template>

            <v-list-item :to="{name: 'settings-users'}">
              <v-list-item-title>{{ $t('users.users') }}</v-list-item-title>
            </v-list-item>

            <v-list-item :to="{name: 'settings-server'}">
              <v-list-item-title>{{ $t('common.settings') }}</v-list-item-title>
            </v-list-item>

            <v-list-item :to="{name: 'settings-ui'}">
              <v-list-item-title>{{ $t('common.ui') }}</v-list-item-title>
            </v-list-item>

            <v-list-item :to="{name: 'metrics'}">
              <v-list-item-title>{{ $t('metrics.title') }}</v-list-item-title>
            </v-list-item>

            <v-list-item :to="{name: 'announcements'}">
              <v-badge
                dot
                inline
                :value="$store.getters.getUnreadAnnouncementsCount()"
                color="info"
              >
                <v-list-item-title>{{ $t('announcements.tab_title') }}</v-list-item-title>
              </v-badge>
            </v-list-item>

            <v-list-item :to="{name: 'updates'}">
              <v-badge
                dot
                inline
                :value="$store.getters.isLatestVersion() == 0"
                color="warning"
              >
                <v-list-item-title>{{ $t('server.updates') }}</v-list-item-title>
              </v-badge>
            </v-list-item>
          </v-list-group>

          <!--   ACCOUNT     -->
          <v-list-group prepend-icon="mdi-account"
                        no-action
                        v-model="expandAccount"
          >
            <template v-slot:activator>
              <v-list-item-title>{{ $t('account_settings.my_account') }}</v-list-item-title>
            </template>

            <v-list-item :to="{name: 'account-me'}">
              <v-list-item-title>{{ $t('account_settings.details') }}</v-list-item-title>
            </v-list-item>

            <v-list-item :to="{name: 'account-api-keys'}">
              <v-list-item-title>{{ $t('users.api_keys') }}</v-list-item-title>
            </v-list-item>

            <v-list-item :to="{name: 'account-settings-ui'}">
              <v-list-item-title>{{ $t('common.ui') }}</v-list-item-title>
            </v-list-item>

            <v-list-item :to="{name: 'account-activity'}">
              <v-list-item-title>{{ $t('users.authentication_activity') }}</v-list-item-title>
            </v-list-item>
          </v-list-group>

          <v-list-item @click="logout">
            <v-list-item-icon>
              <v-icon>mdi-power</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>{{ $t('navigation.logout') }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </v-list>

        <v-divider/>

        <v-list dense class="mt-2">
          <v-list-item>
            <v-list-item-icon>
              <v-icon>{{ themeIcon }}</v-icon>
            </v-list-item-icon>
            <v-select
              class="py-2"
              dense
              v-model="theme"
              :items="themes"
              :label="$t('home.theme')"
            ></v-select>
          </v-list-item>

          <v-list-item>
            <v-list-item-icon>
              <v-icon>mdi-translate</v-icon>
            </v-list-item-icon>
            <v-select
              dense
              class="py-2"
              v-model="locale"
              :items="locales"
              :label="$t('home.translation')"
            >
            </v-select>
          </v-list-item>
        </v-list>

        <v-spacer/>
      </template>

      <template v-slot:append>
        <div v-if="isAdmin && !$_.isEmpty($store.state.actuatorInfo)"
             class="pa-2 pb-6 text-caption"
        >
          <v-badge
            dot
            :value="$store.getters.isLatestVersion() == 0"
            color="warning"
          >
            <router-link :to="{name: 'updates'}" class="link-none">
              版本 {{ $store.state.actuatorInfo.build.version }}
            </router-link>
          </v-badge>
        </div>
      </template>
    </v-navigation-drawer>

    <v-main class="app-shell__main fill-height">
      <reusable-dialogs/>
      <toaster-notification/>
      <router-view/>
    </v-main>

    <v-bottom-navigation
      v-if="isMobile"
      app
      grow
      color="primary"
      class="app-shell__bottom-nav"
      :aria-label="$t('shell.primary_navigation')"
    >
      <v-btn :to="{name: 'dashboard'}">
        <span>{{ $t('navigation.home') }}</span>
        <v-icon>mdi-home</v-icon>
      </v-btn>
      <v-btn :to="{name: 'libraries', params: {libraryId: LIBRARIES_ALL}}">
        <span>{{ $t('navigation.libraries') }}</span>
        <v-icon>mdi-book-multiple</v-icon>
      </v-btn>
      <v-btn :to="{name: 'reading-stats'}">
        <span>{{ $t('reading_stats.title') }}</span>
        <v-icon>mdi-chart-bar</v-icon>
      </v-btn>
      <v-btn :to="{name: 'account-me'}">
        <span>{{ $t('account_settings.my_account') }}</span>
        <v-icon>mdi-account</v-icon>
      </v-btn>
      <v-btn v-if="isAdmin" :to="{name: 'settings-server'}">
        <span>{{ $t('server.tab_title') }}</span>
        <v-icon>mdi-cog</v-icon>
      </v-btn>
    </v-bottom-navigation>
  </div>
</template>

<script lang="ts">
import ReusableDialogs from '@/components/ReusableDialogs.vue'
import LibraryActionsMenu from '@/components/menus/LibraryActionsMenu.vue'
import SearchBox from '@/components/SearchBox.vue'
import {Theme} from '@/types/themes'
import Vue from 'vue'
import {LIBRARIES_ALL} from '@/types/library'
import ToasterNotification from '@/components/ToasterNotification.vue'
import {MediaStatus} from '@/types/enum-books'
import {LibraryDto} from '@/types/komga-libraries'
import {BookSearch, SearchConditionAnyOfBook, SearchConditionMediaStatus, SearchOperatorIs} from '@/types/komga-search'
import LibrariesActionsMenu from '@/components/menus/LibrariesActionsMenu.vue'
import ReorderLibraries from '@/components/ReorderLibraries.vue'

export default Vue.extend({
  name: 'HomeView',
  components: {
    ReorderLibraries,
    LibrariesActionsMenu,
    ToasterNotification,
    LibraryActionsMenu,
    SearchBox,
    ReusableDialogs,
  },
  data: function () {
    return {
      LIBRARIES_ALL,
      drawerVisible: this.$vuetify.breakpoint.width >= 768,
      navigationCollapsed: this.$vuetify.breakpoint.width >= 768 && this.$vuetify.breakpoint.width < 1024,
      locales: this.$i18n.availableLocales.map((x: any) => ({text: this.$i18n.t('common.locale_name', x), value: x})),
      expandSettings: false,
      expandDuplicatePages: false,
      expandMediaManagement: false,
      expandImport: false,
      expandAccount: false,
      expandUnpinned: false,
      showReorder: false,
    }
  },
  async created() {
    if (this.isAdmin) {
      this.$actuator.getInfo()
        .then(x => this.$store.commit('setActuatorInfo', x))
      this.$komgaBooks.getBooksList({
        condition: new SearchConditionAnyOfBook([
          new SearchConditionMediaStatus(new SearchOperatorIs(MediaStatus.ERROR)),
          new SearchConditionMediaStatus(new SearchOperatorIs(MediaStatus.UNSUPPORTED)),
        ]),
      } as BookSearch, {size: 0} as PageRequest)
        .then(x => this.$store.commit('setBooksToCheck', x.totalElements))
      this.$komgaAnnouncements.getAnnouncements()
        .then(x => this.$store.commit('setAnnouncements', x))
      this.$komgaReleases.getReleases()
        .then(x => this.$store.commit('setReleases', x))
    }
    this.checkRoute(this.$route)
  },
  watch: {
    $route(to, from) {
      this.checkRoute(to)
      if (this.isMobile) this.drawerVisible = false
      this.focusRouteHeading()
    },
    '$vuetify.breakpoint.width'(width, previousWidth) {
      if (width < 768) {
        this.drawerVisible = false
        this.navigationCollapsed = false
      } else {
        this.drawerVisible = true
        if (width < 1024) this.navigationCollapsed = true
        else if (previousWidth < 1024) this.navigationCollapsed = false
      }
    },
  },
  computed: {
    isMobile(): boolean {
      return this.$vuetify.breakpoint.width < 768
    },
    isTablet(): boolean {
      return this.$vuetify.breakpoint.width >= 768 && this.$vuetify.breakpoint.width < 1024
    },
    collapseIcon(): string {
      if (this.navigationCollapsed) return this.$vuetify.rtl ? 'mdi-chevron-left' : 'mdi-chevron-right'
      return this.$vuetify.rtl ? 'mdi-chevron-right' : 'mdi-chevron-left'
    },
    navigationToggleLabel(): string {
      if (this.isMobile) return this.drawerVisible ? 'shell.close_navigation' : 'shell.open_navigation'
      return this.navigationCollapsed ? 'shell.expand_navigation' : 'shell.collapse_navigation'
    },
    taskCount(): number {
      return this.$store.state.komgaSse.taskCount
    },
    taskCountByType(): { [key: string]: number } {
      return this.$store.state.komgaSse.taskCountByType
    },
    libraries(): LibraryDto[] {
      return this.$store.getters.getLibraries
    },
    librariesPinned(): LibraryDto[] {
      return this.$store.getters.getLibrariesPinned
    },
    librariesUnpinned(): LibraryDto[] {
      return this.$store.getters.getLibrariesUnpinned
    },
    isAdmin(): boolean {
      return this.$store.getters.meAdmin
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
  },
  methods: {
    checkRoute(to) {
      this.expandSettings = to.path.includes('/settings/')
      this.expandMediaManagement = to.path.includes('/media-management/')
      this.expandImport = to.path.includes('/import/')
      this.expandDuplicatePages = to.path.includes('/duplicate-pages/')
      this.expandAccount = to.path.includes('/account/')
      if (this.librariesUnpinned.some(it => it.id === to.params.libraryId)) this.expandUnpinned = true
      else if (this.librariesPinned.some(it => it.id === to.params.libraryId)) this.expandUnpinned = false
    },
    toggleDrawer() {
      if (this.isMobile) this.drawerVisible = !this.drawerVisible
      else this.navigationCollapsed = !this.navigationCollapsed
    },
    focusRouteHeading() {
      this.$nextTick(() => {
        const target = document.querySelector<HTMLElement>('#main-content h1, #main-content [role="heading"][aria-level="1"], #main-content .text-h4') || document.getElementById('main-content')
        if (target) {
          target.setAttribute('tabindex', '-1')
          target.focus()
        }
      })
    },
    logout() {
      this.$store.dispatch('logout')
      this.$router.push({name: 'login', query: {'logout': true}})
    },
    addLibrary() {
      this.$store.dispatch('dialogAddLibrary')
    },
  },
})
</script>

<style scoped>
.app-shell__drawer {
  margin: 1rem 0 1rem 1rem;
  max-height: calc(100vh - 2rem) !important;
  border: 1px solid var(--k-nav-border) !important;
  border-radius: var(--k-radius-sheet);
  background: var(--k-nav-surface) !important;
  color: var(--k-nav-text) !important;
  box-shadow: var(--k-shadow-floating) !important;
  backdrop-filter: blur(22px);
}

.app-shell__drawer--collapsed {
  width: 80px !important;
  overflow: hidden;
  box-shadow: none !important;
  backdrop-filter: none;
}

.app-shell__drawer--collapsed .app-shell__brand {
  min-height: 80px;
  padding: 0 !important;
  justify-content: center;
}

.app-shell__drawer--collapsed .app-shell__brand ::v-deep .v-list-item__avatar {
  min-width: 44px;
  width: 44px;
  height: 44px;
  margin: 0 !important;
}

.app-shell__drawer--collapsed .app-shell__nav-list {
  padding: var(--k-space-2) 0;
}

.app-shell__drawer--collapsed .app-shell__library-children {
  display: none;
}

.app-shell__drawer--collapsed ::v-deep .v-list-item {
  width: 52px !important;
  min-width: 52px !important;
  min-height: 52px !important;
  margin: var(--k-space-1) auto !important;
  padding: 4px !important;
  display: grid;
  place-items: center;
}

.app-shell__drawer--collapsed ::v-deep .v-list-item__icon,
.app-shell__drawer--collapsed ::v-deep .v-list-group__header__prepend-icon {
  width: 44px;
  min-width: 44px;
  height: 44px;
  margin: 0 !important;
  display: grid;
  place-items: center;
}

.app-shell__drawer--collapsed ::v-deep .v-list-item__icon .v-badge,
.app-shell__drawer--collapsed ::v-deep .v-list-group__header__prepend-icon .v-badge {
  display: inline-grid;
  place-items: center;
}

.app-shell__drawer--collapsed ::v-deep .v-list-item__content,
.app-shell__drawer--collapsed ::v-deep .v-list-item__action,
.app-shell__drawer--collapsed ::v-deep .v-list-group__header__append-icon,
.app-shell__drawer--collapsed ::v-deep .v-list-group__items,
.app-shell__drawer--collapsed ::v-deep .v-navigation-drawer__append {
  display: none !important;
}

.app-shell__drawer--collapsed ::v-deep .v-divider {
  width: 52px;
  margin-inline: auto;
}

.app-shell__bar {
  border-block-end: 1px solid var(--k-border-soft) !important;
  background: var(--k-nav-surface) !important;
  box-shadow: 0 8px 32px rgb(3 12 32 / 10%) !important;
  backdrop-filter: blur(18px);
  z-index: var(--k-z-navigation) !important;
}

.app-shell__wordmark {
  margin-inline: var(--k-space-3) var(--k-space-6);
  color: var(--k-text-primary);
  font-size: var(--k-font-size-section);
  font-weight: 800;
  letter-spacing: -.04em;
}

.app-shell__search {
  min-width: 0;
  max-width: 64rem;
  margin-inline: auto;
}

.app-shell__brand {
  min-height: 6.5rem;
  border-block-end: 1px solid var(--k-nav-border);
  color: var(--k-nav-text) !important;
}

.app-shell__brand-title {
  color: inherit;
  font-size: 1.55rem !important;
  font-weight: 800;
  letter-spacing: -.04em;
}

.app-shell__brand-subtitle {
  color: var(--k-nav-muted) !important;
  font-size: .625rem !important;
  letter-spacing: .14em;
}

.app-shell__nav-list ::v-deep .v-list-item {
  min-height: var(--k-target-min);
  margin: .25rem var(--k-space-3);
  border: 0;
  border-radius: var(--k-radius-control);
  color: var(--k-nav-muted);
}

.app-shell__nav-list ::v-deep .v-list-item::before {
  border-radius: var(--k-radius-control) !important;
}

.app-shell__nav-list ::v-deep .v-list-item__icon {
  color: currentColor !important;
}

.app-shell__library-parent {
  margin-block-start: var(--k-space-2) !important;
  background: var(--k-surface-panel-soft);
  color: var(--k-nav-text) !important;
  font-weight: 750;
}

.app-shell__library-parent ::v-deep .v-list-item__action {
  color: var(--k-nav-muted);
}

.app-shell__library-children {
  margin: 0 var(--k-space-2) var(--k-space-3);
  margin-inline-start: var(--k-space-6);
  padding-inline-start: var(--k-space-2);
}

.app-shell__library-child {
  min-height: var(--k-target-min) !important;
  margin: var(--k-space-1) 0 !important;
  padding-inline: var(--k-space-3) var(--k-space-1) !important;
  border-radius: 10px !important;
  color: var(--k-nav-muted) !important;
  box-shadow: none !important;
  overflow: hidden;
}

.app-shell__library-child ::v-deep .v-list-item__icon {
  min-width: 28px;
  margin: 0;
  margin-inline-end: var(--k-space-2);
}

.app-shell__library-child ::v-deep .v-list-item__title,
.app-shell__library-more ::v-deep .v-list-item__title {
  font-size: .8125rem;
  font-weight: 600;
}

.app-shell__library-child.v-list-item--active {
  background: var(--k-nav-active) !important;
  color: var(--k-primary) !important;
  font-weight: 750;
  box-shadow: none !important;
}

.app-shell__library-more ::v-deep .v-list-group__header {
  min-height: var(--k-target-min);
  margin: var(--k-space-1) 0;
  padding-inline: var(--k-space-3) var(--k-space-1);
  border-radius: 10px;
  color: var(--k-nav-muted);
  box-shadow: none !important;
}

.app-shell__library-child--nested {
  margin-inline-start: var(--k-space-2) !important;
}

.app-shell__main {
  min-width: 0;
  background: var(--k-surface-page);
}

.app-shell__bottom-nav {
  min-height: calc(4rem + env(safe-area-inset-bottom));
  padding-block-end: env(safe-area-inset-bottom);
  border-block-start: 1px solid var(--k-nav-border);
  background: var(--k-nav-surface) !important;
  backdrop-filter: blur(22px);
  box-shadow: var(--k-shadow-floating) !important;
  z-index: var(--k-z-navigation) !important;
}

.app-shell__bottom-nav ::v-deep .v-btn {
  min-width: var(--k-target-min) !important;
  min-height: var(--k-target-min) !important;
  padding-inline: var(--k-space-1) !important;
  font-size: 0.6875rem !important;
}

.app-shell__bottom-nav ::v-deep .v-btn__content > span {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.v-list-item--active {
  border-inline-start: 0;
  background: var(--k-nav-active) !important;
  color: var(--k-primary) !important;
  font-weight: 700;
}

@media (max-width: 47.9375rem) {
  .app-shell__drawer {
    margin: 0;
    max-height: 100vh !important;
    border-radius: 0;
  }
  .app-shell__bar {
    padding-inline: var(--k-space-1) !important;
  }
}
</style>
