<template>
  <div>
    <v-bottom-navigation
      v-if="bottomNavigation"
      grow color="primary"
      :app="$vuetify.breakpoint.smAndUp"
      :fixed="bottomNavigation"
      class="library-nav library-nav--bottom"
    >
      <v-btn v-if="showRecommended"
             :to="{name: 'recommended-libraries', params: {libraryId: libraryId}}"
      >
        <span class="caption">{{ $t('library_navigation.recommended') }}</span>
        <v-icon>mdi-star</v-icon>
      </v-btn>

      <v-btn :to="{name: 'browse-libraries', params: {libraryId: libraryId}}">
        <span class="caption">{{ $t('library_navigation.browse_series') }}</span>
        <v-icon>mdi-bookshelf</v-icon>
      </v-btn>

      <v-btn :to="{name: 'browse-books', params: {libraryId: libraryId}}">
        <span class="caption">{{ $t('library_navigation.browse_books') }}</span>
        <v-icon>mdi-book-multiple</v-icon>
      </v-btn>

      <v-btn
        v-if="collectionsCount > 0"
        :to="{name: 'browse-collections', params: {libraryId: libraryId}}"
      >
        <span class="caption">{{ $t('library_navigation.collections') }}</span>
        <v-icon>mdi-layers-triple</v-icon>
      </v-btn>

      <v-btn
        v-if="readListsCount > 0"
        :to="{name: 'browse-readlists', params: {libraryId: libraryId}}"
      >
        <span class="caption">{{ $t('library_navigation.readlists') }}</span>
        <v-icon>mdi-bookmark-multiple</v-icon>
      </v-btn>

    </v-bottom-navigation>

    <template
      v-if="!bottomNavigation"
    >
      <v-btn v-if="showRecommended"
             :to="{name: 'recommended-libraries', params: {libraryId: libraryId}}"
             text
             class="mx-1 library-nav__button"
      >
        {{ $t('library_navigation.recommended') }}
      </v-btn>

      <v-btn :to="{name: 'browse-libraries', params: {libraryId: libraryId}}"
             text
             class="mx-1 library-nav__button"
      >
        {{ $t('library_navigation.browse_series') }}
      </v-btn>

      <v-btn :to="{name: 'browse-books', params: {libraryId: libraryId}}"
             text
             class="mx-1 library-nav__button"
      >
        {{ $t('library_navigation.browse_books') }}
      </v-btn>

      <v-btn
        v-if="collectionsCount > 0"
        :to="{name: 'browse-collections', params: {libraryId: libraryId}}"
        text
        class="mx-1 library-nav__button"
      >
        {{ $t('library_navigation.collections') }}
      </v-btn>

      <v-btn
        v-if="readListsCount > 0"
        :to="{name: 'browse-readlists', params: {libraryId: libraryId}}"
        text
        class="mx-1 library-nav__button"
      >
        {{ $t('library_navigation.readlists') }}
      </v-btn>

    </template>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import {COLLECTION_ADDED, COLLECTION_DELETED, READLIST_ADDED, READLIST_DELETED} from '@/types/events'
import {LIBRARIES_ALL} from '@/types/library'
import {LibraryDto} from '@/types/komga-libraries'

export default Vue.extend({
  name: 'LibraryNavigation',
  data: () => {
    return {
      collectionsCount: 0,
      readListsCount: 0,
    }
  },
  props: {
    libraryId: {
      type: String,
      required: true,
    },
    bottomNavigation: {
      type: Boolean,
      default: false,
    },
  },
  watch: {
    libraryId: {
      handler(val) {
        this.loadReadListCounts(val)
        this.loadCollectionCounts(val)
      },
      immediate: true,
    },
    '$store.getters.getLibrariesPinned': {
      handler(val) {
        if (this.libraryId === LIBRARIES_ALL) {
          this.loadCollectionCounts(this.libraryId)
          this.loadReadListCounts(this.libraryId)
        }
      },
    },
  },
  created() {
    this.$eventHub.$on(COLLECTION_ADDED, this.collectionAdded)
    this.$eventHub.$on(COLLECTION_DELETED, this.collectionDeleted)
    this.$eventHub.$on(READLIST_ADDED, this.readListAdded)
    this.$eventHub.$on(READLIST_DELETED, this.readListDeleted)
  },
  beforeDestroy() {
    this.$eventHub.$off(COLLECTION_ADDED, this.collectionAdded)
    this.$eventHub.$off(COLLECTION_DELETED, this.collectionDeleted)
    this.$eventHub.$off(READLIST_ADDED, this.readListAdded)
    this.$eventHub.$off(READLIST_DELETED, this.readListDeleted)
  },
  computed: {
    showRecommended(): boolean {
      return this.libraryId !== LIBRARIES_ALL
    },
  },
  methods: {
    readListAdded() {
      if(this.readListsCount === 0) this.loadReadListCounts(this.libraryId)
    },
    readListDeleted() {
      if(this.readListsCount === 1) this.loadReadListCounts(this.libraryId)
    },
    collectionAdded() {
      if(this.collectionsCount === 0) this.loadCollectionCounts(this.libraryId)
    },
    collectionDeleted() {
      if(this.collectionsCount === 1) this.loadCollectionCounts(this.libraryId)
    },
    async loadCollectionCounts(libraryId: string) {
      const lib = libraryId !== LIBRARIES_ALL ? [libraryId] : this.$store.getters.getLibrariesPinned.map((it: LibraryDto) => it.id)
      this.$komgaCollections.getCollections(lib, {size: 0})
      .then(v => this.collectionsCount = v.totalElements)
    },
    async loadReadListCounts(libraryId: string) {
      const lib = libraryId !== LIBRARIES_ALL ? [libraryId] : this.$store.getters.getLibrariesPinned.map((it: LibraryDto) => it.id)
      await this.$komgaReadLists.getReadLists(lib, {size: 0})
        .then(v => this.readListsCount = v.totalElements)
    },
  },
})
</script>

<style scoped>
.library-nav__button {
  min-height: 44px;
  border-radius: 14px;
  color: var(--k-text-secondary);
  letter-spacing: .01em;
}

.library-nav__button.v-btn--active {
  background: color-mix(in srgb, var(--k-primary) 14%, transparent);
  color: var(--k-primary);
}

.library-nav--bottom {
  margin: 0 auto 8px;
  width: min(calc(100% - 20px), 680px) !important;
  border: 1px solid color-mix(in srgb, var(--k-primary) 18%, transparent);
  border-radius: 20px 20px 12px 12px;
  background: color-mix(in srgb, var(--k-surface-card) 88%, transparent) !important;
  box-shadow: 0 16px 42px rgba(3, 10, 30, .34) !important;
  backdrop-filter: blur(18px) saturate(140%);
  overflow: hidden;
}

.library-nav--bottom ::v-deep .v-btn {
  min-width: 44px !important;
  min-height: 56px;
}

</style>
