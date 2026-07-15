<template>
  <section class="series-directories" :aria-label="$t('browse_series.directory_breadcrumb_label')">
    <nav class="series-directories__breadcrumbs" :aria-label="$t('browse_series.directory_breadcrumb_label')">
      <button
        type="button"
        class="series-directories__crumb k-touch-target"
        :class="{'series-directories__crumb--current': !currentPath}"
        :aria-current="!currentPath ? 'page' : undefined"
        @click="$emit('navigate', '')"
      >
        <v-icon size="20" aria-hidden="true">mdi-home-variant-outline</v-icon>
        <span>{{ rootLabel }}</span>
      </button>

      <template v-for="crumb in breadcrumbs">
        <v-icon :key="`${crumb.path}-separator`" size="18" class="series-directories__separator" aria-hidden="true">
          mdi-chevron-right
        </v-icon>
        <button
          :key="crumb.path"
          type="button"
          class="series-directories__crumb k-touch-target"
          :class="{'series-directories__crumb--current': crumb.path === currentPath}"
          :aria-current="crumb.path === currentPath ? 'page' : undefined"
          @click="$emit('navigate', crumb.path)"
        >
          {{ crumb.name }}
        </button>
      </template>
    </nav>

    <div v-if="directories.length" class="series-directories__grid">
      <button
        v-for="directory in directories"
        :key="directory.path"
        type="button"
        class="series-directory-card"
        :aria-label="$t('browse_series.open_directory', {name: directory.name})"
        @click="$emit('navigate', directory.path)"
      >
        <span class="series-directory-card__visual" aria-hidden="true">
          <v-img
            v-if="directory.thumbnailBookId"
            :src="bookThumbnailUrl(directory.thumbnailBookId)"
            aspect-ratio="1.6"
            class="series-directory-card__cover"
          >
            <template v-slot:placeholder>
              <span class="series-directory-card__placeholder"><v-icon size="34">mdi-folder-image</v-icon></span>
            </template>
          </v-img>
          <span v-else class="series-directory-card__placeholder"><v-icon size="38">mdi-folder-outline</v-icon></span>
          <span class="series-directory-card__folder"><v-icon size="24">mdi-folder</v-icon></span>
        </span>
        <span class="series-directory-card__body">
          <strong>{{ directory.name }}</strong>
          <span>{{ $tc('common.books_n', directory.descendantBooksCount, {count: directory.descendantBooksCount}) }}</span>
        </span>
        <v-icon class="series-directory-card__arrow" size="22" aria-hidden="true">mdi-chevron-right</v-icon>
      </button>
    </div>
  </section>
</template>

<script lang="ts">
import Vue from 'vue'
import {bookThumbnailUrl} from '@/functions/urls'
import {SeriesDirectoryBreadcrumbDto, SeriesDirectoryDto} from '@/types/komga-directories'

export default Vue.extend({
  name: 'SeriesDirectoryBrowser',
  props: {
    rootLabel: {
      type: String,
      required: true,
    },
    currentPath: {
      type: String,
      default: '',
    },
    breadcrumbs: {
      type: Array as () => SeriesDirectoryBreadcrumbDto[],
      default: () => [],
    },
    directories: {
      type: Array as () => SeriesDirectoryDto[],
      default: () => [],
    },
  },
  methods: {
    bookThumbnailUrl,
  },
})
</script>

<style scoped>
.series-directories {
  display: grid;
  gap: var(--k-space-4);
  margin: var(--k-space-4) 0 var(--k-space-6);
}

.series-directories__breadcrumbs {
  align-items: center;
  display: flex;
  min-height: var(--k-target-min);
  overflow-x: auto;
  overscroll-behavior-inline: contain;
  scrollbar-width: thin;
}

.series-directories__crumb {
  align-items: center;
  background: transparent;
  border: 0;
  border-radius: var(--k-radius-control);
  color: var(--k-text-secondary);
  cursor: pointer;
  display: inline-flex;
  flex: 0 0 auto;
  font: inherit;
  font-size: var(--k-font-size-label);
  gap: var(--k-space-2);
  padding: 0 var(--k-space-3);
}

.series-directories__crumb:hover,
.series-directories__crumb:focus-visible,
.series-directories__crumb--current {
  background: var(--k-surface-muted);
  color: var(--k-text-primary);
}

.series-directories__crumb:focus-visible,
.series-directory-card:focus-visible {
  outline: var(--k-focus-width) solid var(--k-focus);
  outline-offset: var(--k-focus-offset);
}

.series-directories__separator {
  color: var(--k-text-secondary);
  flex: 0 0 auto;
}

.series-directories__grid {
  display: grid;
  gap: var(--k-space-3);
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 17rem), 1fr));
}

.series-directory-card {
  align-items: center;
  background: var(--k-surface-card);
  border: 1px solid var(--k-border-soft);
  border-radius: var(--k-radius-card);
  box-shadow: var(--k-shadow-card);
  color: var(--k-text-primary);
  cursor: pointer;
  display: grid;
  gap: var(--k-space-3);
  grid-template-columns: 5rem minmax(0, 1fr) var(--k-target-min);
  min-height: 6.25rem;
  overflow: hidden;
  padding: var(--k-space-2);
  text-align: start;
  transition: border-color var(--k-motion-fast) var(--k-ease-standard), transform var(--k-motion-fast) var(--k-ease-standard);
  width: 100%;
}

.series-directory-card:hover {
  border-color: var(--k-primary);
  transform: translateY(-2px);
}

.series-directory-card__visual {
  background: var(--k-surface-muted);
  border-radius: calc(var(--k-radius-card) - .25rem);
  display: block;
  height: 5rem;
  overflow: hidden;
  position: relative;
  width: 5rem;
}

.series-directory-card__cover,
.series-directory-card__placeholder {
  height: 100%;
  width: 100%;
}

.series-directory-card__placeholder {
  align-items: center;
  color: var(--k-primary);
  display: flex;
  justify-content: center;
}

.series-directory-card__folder {
  align-items: center;
  background: var(--k-surface-card);
  border-radius: var(--k-radius-pill);
  bottom: .25rem;
  color: var(--k-primary);
  display: flex;
  height: 2rem;
  justify-content: center;
  position: absolute;
  right: .25rem;
  width: 2rem;
}

.series-directory-card__body {
  display: grid;
  gap: var(--k-space-1);
  min-width: 0;
}

.series-directory-card__body strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.series-directory-card__body span {
  color: var(--k-text-secondary);
  font-size: var(--k-font-size-meta);
}

.series-directory-card__arrow {
  color: var(--k-text-secondary);
  justify-self: center;
}

@media (max-width: 37.5rem) {
  .series-directory-card {
    grid-template-columns: 4rem minmax(0, 1fr) var(--k-target-min);
    min-height: 5.25rem;
  }

  .series-directory-card__visual {
    height: 4rem;
    width: 4rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .series-directory-card {
    transition: none;
  }

  .series-directory-card:hover {
    transform: none;
  }
}
</style>
