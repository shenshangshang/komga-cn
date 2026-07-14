<template>
  <v-hover :disabled="disableHover">
    <template v-slot:default="{ hover }">
      <v-card
        :width="width"
        class="item-card-surface"
        @click="onClick"
        :class="{'no-link': noLink, 'item-card-surface--selected': selected}"
        :ripple="false"
        :role="cardRole"
        :tabindex="cardTabIndex"
        :aria-label="displayTitle"
        :aria-checked="preselect && onSelected ? selected.toString() : undefined"
        @keydown.enter.prevent="onKeyboardActivate"
        @keydown.space.prevent="onKeyboardActivate"
        @focusin="focusWithin = true"
        @focusout="onFocusOut"
      >
        <!--      Thumbnail-->
        <v-img
          :src="thumbnailUrl"
          :lazy-src="thumbnailError ? coverBase64 : undefined"
          aspect-ratio="0.7071"
          :contain="!isStretch"
          :position="isStretch ? stretchMode : undefined"
          :class="['item-card__cover', {'blur': shouldBlurPoster}]"
          @error="thumbnailError = true"
          @load="thumbnailError = false"
        >
          <!-- unread tick for book -->
          <div class="unread" v-if="isUnread"/>

          <!-- unread count for series -->
          <span v-else-if="unreadCount"
                class="item-card__unread-count pa-1 px-2 text-subtitle-2"
          >
            {{ unreadCount }}
          </span>

          <!-- Thumbnail overlay -->
          <v-fade-transition>
            <v-overlay
              v-if="showOverlay(hover)"
              absolute
              :opacity="hover || focusWithin || actionMenuState ? 0.3 : 0"
              :class="`${hover || focusWithin || actionMenuState ? 'item-border-darken' : selected ? 'item-border' : 'item-border-transparent'} overlay-full`"
            >
              <!-- Circle icon for selection (top left) -->
              <v-btn v-if="onSelected"
                      icon
                      class="item-card__select k-touch-target"
                      :color="selected ? 'secondary' : ''"
                      :aria-label="$t(selected ? 'item_card.deselect' : 'item_card.select')"
                      @click.stop="selectItem"
              >
                <v-icon>{{ selected || (preselect && hover) ? 'mdi-checkbox-marked-circle' : 'mdi-checkbox-blank-circle-outline' }}</v-icon>
              </v-btn>

              <!-- FAB incognito reading (top right) -->
              <v-btn
                v-if="showIncognitoFab"
                fab
                small
                color="grey darken-3"
                :aria-label="$t('browse_book.read_incognito')"
                :style="'position: absolute; top: 5px; ' + ($vuetify.rtl ? 'left' : 'right') + ': 10px'"
                :to="incognitoFabTo"
                @click.native="$event.stopImmediatePropagation()"
              >
                <v-icon small>mdi-incognito</v-icon>
              </v-btn>

              <!-- FAB reading (center) -->
              <v-btn
                v-if="showFab"
                fab
                x-large
                color="accent"
                :aria-label="$t('browse_book.read_book')"
                style="position: absolute; top: 50%; left: 50%; margin-left: -36px; margin-top: -36px"
                :to="fabTo"
                @click.native="$event.stopImmediatePropagation()"
              >
                <v-icon>mdi-book-open-page-variant</v-icon>
              </v-btn>

              <!-- Pen icon for edition (bottom left) -->
              <v-btn icon
                     v-if="!selected && !preselect && onEdit"
                     class="k-touch-target"
                     :aria-label="$t('menu.edit')"
                     :style="'position: absolute; bottom: 5px; ' + ($vuetify.rtl ? 'right' : 'left' ) +': 5px'"
                     @click.stop="editItem"
              >
                <v-icon>mdi-pencil</v-icon>
              </v-btn>

              <!-- Action menu (bottom right) -->
              <div v-if="!selected && !preselect && actionMenu"
                   :style="'position: absolute; bottom: 5px; ' + ($vuetify.rtl ? 'left' : 'right') +': 5px'"
              >
                <one-shot-actions-menu v-if="computedItem.type() === ItemTypes.BOOK && item.oneshot"
                                       :book="item"
                                       :menu.sync="actionMenuState"
                />
                <book-actions-menu v-if="computedItem.type() === ItemTypes.BOOK && !item.oneshot"
                                   :book="item"
                                   :menu.sync="actionMenuState"
                />
                <one-shot-actions-menu v-if="computedItem.type() === ItemTypes.SERIES && item.oneshot"
                                       :series="item"
                                       :menu.sync="actionMenuState"
                />
                <series-actions-menu v-if="computedItem.type() === ItemTypes.SERIES && !item.oneshot"
                                     :series="item"
                                     :menu.sync="actionMenuState"
                />
                <collection-actions-menu v-if="computedItem.type() === ItemTypes.COLLECTION"
                                         :collection="item"
                                         :menu.sync="actionMenuState"
                />
                <read-list-actions-menu v-if="computedItem.type() === ItemTypes.READLIST"
                                        :read-list="item"
                                        :menu.sync="actionMenuState"
                />
              </div>
            </v-overlay>
          </v-fade-transition>
          <v-progress-linear
            v-if="isInProgress"
            :value="readProgressPercentage"
            color="accent"
            height="6"
            :aria-label="`${$t('item_card.progress')}: ${Math.round(readProgressPercentage)}%`"
            style="position: absolute; bottom: 0"
          />
        </v-img>

        <!--      Description-->
        <template v-if="!thumbnailOnly">
          <router-link v-if="!Array.isArray(title)" :to="title.to" class="link-underline"
                       @click.native="$event.stopImmediatePropagation()">
            <v-card-subtitle
              v-line-clamp="2"
              v-bind="subtitleProps"
              :title="title.title"
            >{{ title.title }}
            </v-card-subtitle>
          </router-link>
          <template v-if="Array.isArray(title)">
            <v-card-subtitle
              v-bind="subtitleProps"
            >
              <router-link
                v-for="(t, i) in title"
                :key="i"
                :to="t.to"
                @click.native="$event.stopImmediatePropagation()"
                class="link-underline text-truncate"
                :title="t.title"
                style="display: block"
                :class="i !== 0 ? 'font-weight-light' : ''"
              >{{ t.title }}
              </router-link>
            </v-card-subtitle>
          </template>
          <v-card-text class="px-2 pt-0 font-weight-light" v-html="sanitizeRichHtml(body)">
          </v-card-text>
        </template>
      </v-card>
    </template>
  </v-hover>
</template>

<script lang="ts">
import BookActionsMenu from '@/components/menus/BookActionsMenu.vue'
import CollectionActionsMenu from '@/components/menus/CollectionActionsMenu.vue'
import SeriesActionsMenu from '@/components/menus/SeriesActionsMenu.vue'
import {getReadProgress, getReadProgressPercentage} from '@/functions/book-progress'
import {ReadStatus} from '@/types/enum-books'
import {createItem, Item, ItemContext, ItemTitle, ItemTypes} from '@/types/items'
import Vue from 'vue'
import {sanitizeRichHtml} from '@/functions/sanitize-html'
import {RawLocation} from 'vue-router'
import ReadListActionsMenu from '@/components/menus/ReadListActionsMenu.vue'
import {BookDto} from '@/types/komga-books'
import {SeriesDto} from '@/types/komga-series'
import {
  THUMBNAILBOOK_ADDED,
  THUMBNAILBOOK_DELETED,
  THUMBNAILCOLLECTION_ADDED,
  THUMBNAILCOLLECTION_DELETED,
  THUMBNAILREADLIST_ADDED,
  THUMBNAILREADLIST_DELETED,
  THUMBNAILSERIES_ADDED,
  THUMBNAILSERIES_DELETED,
} from '@/types/events'
import {
  ThumbnailBookSseDto,
  ThumbnailCollectionSseDto,
  ThumbnailReadListSseDto,
  ThumbnailSeriesSseDto,
} from '@/types/komga-sse'
import {coverBase64} from '@/types/image'
import {ReadListDto} from '@/types/komga-readlists'
import OneShotActionsMenu from '@/components/menus/OneshotActionsMenu.vue'
import {CLIENT_SETTING} from '@/types/komga-clientsettings'

export default Vue.extend({
  name: 'ItemCard',
  data: () => ({sanitizeRichHtml}),
  components: {OneShotActionsMenu, BookActionsMenu, SeriesActionsMenu, CollectionActionsMenu, ReadListActionsMenu},
  props: {
    item: {
      type: Object as () => BookDto | SeriesDto | CollectionDto | ReadListDto,
      required: true,
    },
    itemContext: {
      type: Array as () => ItemContext[],
      default: () => [],
    },
    // hide the bottom part of the card
    thumbnailOnly: {
      type: Boolean,
      default: false,
    },
    // disables the default link on clicking the card
    noLink: {
      type: Boolean,
      default: false,
    },
    width: {
      type: [String, Number],
      required: false,
      default: 150,
    },
    // when true, card will show the active border and circle icon full
    selected: {
      type: Boolean,
      default: false,
    },
    // when true, will display the border like if the card was hovered, and click anywhere will trigger onSelected
    preselect: {
      type: Boolean,
      required: false,
    },
    // callback function to call when selecting the card
    onSelected: {
      type: Function,
      default: undefined,
      required: false,
    },
    // callback function for the edit button
    onEdit: {
      type: Function,
      default: undefined,
      required: false,
    },
    // action menu enabled or not
    actionMenu: {
      type: Boolean,
      default: true,
    },
    // force disable fab
    disableFab: {
      type: Boolean,
      default: false,
    },
  },
  data: () => {
    return {
      ItemTypes,
      actionMenuState: false,
      thumbnailError: false,
      thumbnailCacheBust: '',
      focusWithin: false,
      coverBase64,
    }
  },
  created() {
    this.$eventHub.$on(THUMBNAILBOOK_ADDED, this.thumbnailBookChanged)
    this.$eventHub.$on(THUMBNAILBOOK_DELETED, this.thumbnailBookChanged)

    this.$eventHub.$on(THUMBNAILSERIES_ADDED, this.thumbnailSeriesChanged)
    this.$eventHub.$on(THUMBNAILSERIES_DELETED, this.thumbnailSeriesChanged)

    this.$eventHub.$on(THUMBNAILREADLIST_ADDED, this.thumbnailReadListChanged)
    this.$eventHub.$on(THUMBNAILREADLIST_DELETED, this.thumbnailReadListChanged)

    this.$eventHub.$on(THUMBNAILCOLLECTION_ADDED, this.thumbnailCollectionChanged)
    this.$eventHub.$on(THUMBNAILCOLLECTION_DELETED, this.thumbnailCollectionChanged)
  },
  beforeDestroy() {
    this.$eventHub.$off(THUMBNAILBOOK_ADDED, this.thumbnailBookChanged)
    this.$eventHub.$off(THUMBNAILBOOK_DELETED, this.thumbnailBookChanged)

    this.$eventHub.$off(THUMBNAILSERIES_ADDED, this.thumbnailSeriesChanged)
    this.$eventHub.$off(THUMBNAILSERIES_DELETED, this.thumbnailSeriesChanged)

    this.$eventHub.$off(THUMBNAILREADLIST_ADDED, this.thumbnailReadListChanged)
    this.$eventHub.$off(THUMBNAILREADLIST_DELETED, this.thumbnailReadListChanged)

    this.$eventHub.$off(THUMBNAILCOLLECTION_ADDED, this.thumbnailCollectionChanged)
    this.$eventHub.$off(THUMBNAILCOLLECTION_DELETED, this.thumbnailCollectionChanged)
  },
  computed: {
    isStretch(): boolean {
      return this.$store.getters.getClientSettings[CLIENT_SETTING.WEBUI_POSTER_STRETCH]?.value === 'true'
    },
    stretchMode(): string {
      return this.$store.getters.getClientSettings[CLIENT_SETTING.WEBUI_POSTER_STRETCH_MODE]?.value || 'top'
    },
    isBlurUnread(): boolean {
      return this.$store.getters.getClientSettings[CLIENT_SETTING.WEBUI_POSTER_BLUR_UNREAD]?.value === 'true'
    },
    shouldBlurPoster(): boolean | undefined {
      return (this.isUnread || this.allUnread) && this.isBlurUnread
    },
    canReadPages(): boolean {
      return this.$store.getters.mePageStreaming && this.computedItem.type() === ItemTypes.BOOK
    },
    overlay(): boolean {
      return this.onEdit !== undefined || this.onSelected !== undefined || this.showFab || this.actionMenu
    },
    computedItem(): Item<BookDto | SeriesDto | CollectionDto | ReadListDto> {
      let item = this.item
      if ('libraryId' in this.item && this.$store.getters.getLibraryById((this.item as any).libraryId).unavailable)
        item = {...item, deleted: true}
      return createItem(item)
    },
    disableHover(): boolean {
      return !this.overlay
    },
    persistentActions(): boolean {
      return this.$vuetify.breakpoint.smAndDown
    },
    displayTitle(): string {
      const titles = Array.isArray(this.title) ? this.title : [this.title]
      return titles.map(value => value.title).join(' – ')
    },
    cardRole(): string | undefined {
      if (this.preselect && this.onSelected) return 'checkbox'
      if (!this.noLink) return 'link'
      return undefined
    },
    cardTabIndex(): number | undefined {
      return (this.preselect && this.onSelected) || !this.noLink ? 0 : undefined
    },
    thumbnailUrl(): string {
      return this.computedItem.thumbnailUrl() + this.thumbnailCacheBust
    },
    title(): ItemTitle | ItemTitle[] {
      return this.computedItem.title(this.itemContext)
    },
    subtitleProps(): Object {
      return this.computedItem.subtitleProps()
    },
    body(): string {
      return this.computedItem.body(this.itemContext)
    },
    isInProgress(): boolean {
      if (this.computedItem.type() === ItemTypes.BOOK) return getReadProgress(this.item as BookDto) === ReadStatus.IN_PROGRESS
      return false
    },
    isUnread(): boolean {
      if (this.computedItem.type() === ItemTypes.BOOK) return getReadProgress(this.item as BookDto) === ReadStatus.UNREAD
      if (this.computedItem.type() === ItemTypes.SERIES && (this.item as SeriesDto).oneshot) return (this.item as SeriesDto).booksUnreadCount + (this.item as SeriesDto).booksInProgressCount > 0
      return false
    },
    unreadCount(): number | undefined {
      if (this.computedItem.type() === ItemTypes.SERIES) return (this.item as SeriesDto).booksUnreadCount + (this.item as SeriesDto).booksInProgressCount
      return undefined
    },
    allUnread(): boolean | undefined {
      if (this.computedItem.type() === ItemTypes.SERIES) return (this.item as SeriesDto).booksCount == (this.item as SeriesDto).booksUnreadCount
      return undefined
    },
    readProgressPercentage(): number {
      if (this.computedItem.type() === ItemTypes.BOOK) return getReadProgressPercentage(this.item as BookDto)
      return 0
    },
    bookReady(): boolean {
      if (this.computedItem.type() === ItemTypes.BOOK) {
        return (this.item as BookDto).media.status === 'READY'
      }
      return false
    },
    showFab(): boolean {
      return !this.disableFab && this.bookReady && !this.selected && !this.preselect && this.canReadPages
    },
    showIncognitoFab(): boolean {
      return !this.disableFab && this.bookReady && !this.selected && !this.preselect && this.canReadPages
    },
    to(): RawLocation {
      return this.computedItem.to()
    },
    fabTo(): RawLocation {
      return this.computedItem.fabTo()
    },
    incognitoFabTo(): RawLocation {
      const fabTo = this.computedItem.fabTo()
      if (fabTo && typeof fabTo === 'object' && fabTo.query) {
        return {
          ...fabTo,
          query: {
            ...fabTo.query,
            incognito: 'true',
          },
        }
      } else if (fabTo && typeof fabTo === 'object') {
        return {
          ...fabTo,
          query: { incognito: 'true' },
        }
      }
      return fabTo
    },
  },
  methods: {
    showOverlay(hover: boolean): boolean {
      return hover || this.focusWithin || this.persistentActions || this.selected || this.preselect || this.actionMenuState
    },
    onFocusOut(event: FocusEvent) {
      const next = event.relatedTarget as Node | null
      if (!next || !(event.currentTarget as HTMLElement).contains(next)) this.focusWithin = false
    },
    onKeyboardActivate(event: KeyboardEvent) {
      if (this.preselect && this.onSelected !== undefined) this.selectItem(event as unknown as MouseEvent)
      else if (!this.noLink) this.goto()
    },
    thumbnailBookChanged(event: ThumbnailBookSseDto) {
      if (event.selected && (this.computedItem.type() === ItemTypes.BOOK && event.bookId === this.item.id)
        || (this.thumbnailError && this.computedItem.type() === ItemTypes.SERIES && event.seriesId === this.item.id)
      ) {
        this.thumbnailCacheBust = '?' + this.$_.random(1000)
      }
    },
    thumbnailSeriesChanged(event: ThumbnailSeriesSseDto) {
      if (event.selected && this.computedItem.type() === ItemTypes.SERIES && event.seriesId === this.item.id) {
        this.thumbnailCacheBust = '?' + this.$_.random(1000)
      }
    },
    thumbnailReadListChanged(event: ThumbnailReadListSseDto) {
      if (event.selected && this.computedItem.type() === ItemTypes.READLIST && event.readListId === this.item.id) {
        this.thumbnailCacheBust = '?' + this.$_.random(1000)
      }
    },
    thumbnailCollectionChanged(event: ThumbnailCollectionSseDto) {
      if (event.selected && this.computedItem.type() === ItemTypes.COLLECTION && event.collectionId === this.item.id) {
        this.thumbnailCacheBust = '?' + this.$_.random(1000)
      }
    },
    onClick(e: MouseEvent) {
      if (this.preselect && this.onSelected !== undefined) {
        this.selectItem(e)
      } else if (!this.noLink) {
        this.goto()
      }
    },
    goto() {
      this.$router.push(this.computedItem.to())
    },
    selectItem(e: MouseEvent) {
      if (this.onSelected !== undefined) {
        this.onSelected(this.item, e)
      }
    },
    editItem() {
      if (this.onEdit !== undefined) {
        this.onEdit(this.item)
      }
    },
  },
})
</script>

<style scoped>
.item-card-surface {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--k-text-primary);
  border-inline-start: 5px solid var(--k-accent-progress);
  border-radius: var(--k-radius-card) !important;
  background: var(--k-surface-card);
  box-shadow: var(--k-shadow-card);
  transition: transform var(--k-motion-standard) var(--k-ease-standard), box-shadow var(--k-motion-standard) var(--k-ease-standard);
}

.item-card-surface:focus-within,
.item-card-surface--selected {
  border-color: var(--k-primary);
  border-inline-start-color: var(--k-primary);
  box-shadow: var(--k-shadow-card-active);
}

.item-card__cover {
  aspect-ratio: var(--k-cover-aspect-ratio);
  border-block-end: 1px solid var(--k-text-primary);
  background: var(--k-surface-muted);
}

.item-card-surface ::v-deep .v-card__subtitle {
  color: var(--k-text-primary) !important;
  font-family: var(--k-font-display);
  font-weight: 700;
  line-height: 1.25;
}

.item-card__unread-count {
  position: absolute;
  inset-inline-end: 0;
  border: 1px solid var(--k-accent-progress);
  background: var(--k-surface-card);
  color: var(--k-text-primary);
  font-weight: 700;
}

.item-card__select {
  position: absolute;
  inset-block-start: var(--k-space-1);
  inset-inline-start: var(--k-space-2);
}

.blur ::v-deep .v-image__image {
  filter: blur(5px);
}

.no-link {
  cursor: default;
}

.item-border {
  border: 3px solid var(--k-primary);
}

.item-border-transparent {
  border: 3px solid transparent;
}

.item-border-darken {
  border: 3px solid var(--k-text-primary);
}

.overlay-full ::v-deep .v-overlay__content {
  width: 100%;
  height: 100%;
}

.unread {
  border-left: 25px solid transparent;
  border-right: 25px solid var(--k-accent-progress);
  border-bottom: 25px solid transparent;
  height: 0;
  width: 0;
  position: absolute;
  right: 0;
  z-index: 2;
}

.item-card-surface ::v-deep .v-image__image {
  transition: transform var(--k-motion-standard) var(--k-ease-standard);
}

@media (hover: hover) {
  .item-card-surface:hover {
    transform: translate(-2px, -2px);
    box-shadow: var(--k-shadow-card-active);
  }

  .item-card-surface:hover ::v-deep .v-image__image {
    transform: scale(1.035);
  }
}
</style>
