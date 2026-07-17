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

    <div
      v-if="directories.length"
      ref="content"
      v-resize="onResize"
      class="series-directories__grid"
    >
      <button
        v-for="directory in directories"
        :key="directory.path"
        type="button"
        class="series-directory-card"
        :style="{width: `${cardWidth}px`}"
        :aria-label="$t('browse_series.open_directory', {name: directory.name})"
        @click="$emit('navigate', directory.path)"
      >
        <span class="series-directory-card__visual" aria-hidden="true">
          <v-img
            v-if="directory.thumbnailBookId"
            :src="bookThumbnailUrl(directory.thumbnailBookId)"
            aspect-ratio="0.7071"
            class="series-directory-card__cover"
          >
            <template v-slot:placeholder>
              <span class="series-directory-card__placeholder"/>
            </template>
          </v-img>
          <span v-else class="series-directory-card__placeholder"/>
        </span>
        <span class="series-directory-card__body">
          <strong>{{ directory.name }}</strong>
          <span>{{ $tc('common.books_n', directory.descendantBooksCount, {count: directory.descendantBooksCount}) }}</span>
        </span>
      </button>
    </div>
  </section>
</template>

<script lang="ts">
import Vue from 'vue'
import {bookThumbnailUrl} from '@/functions/urls'
import {SeriesDirectoryBreadcrumbDto, SeriesDirectoryDto} from '@/types/komga-directories'
import {computeCardWidth} from '@/functions/grid-utilities'

export default Vue.extend({
  name: 'SeriesDirectoryBrowser',
  data: () => ({
    cardWidth: 150,
  }),
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
    onResize() {
      const content = this.$refs.content as HTMLElement | undefined
      if (content) this.cardWidth = computeCardWidth(content.clientWidth, this.$vuetify.breakpoint.name.toString())
    },
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
  display: flex;
  flex-wrap: wrap;
  padding: var(--k-space-2) clamp(0px, 1vw, var(--k-space-3));
}

.series-directory-card {
  background: var(--k-surface-card);
  border: 1px solid color-mix(in srgb, var(--k-primary) 14%, transparent);
  border-radius: var(--k-radius-card);
  box-shadow: 0 14px 36px rgba(3, 10, 30, 0.2);
  color: var(--k-text-primary);
  cursor: pointer;
  display: flex;
  flex: 0 0 auto;
  flex-direction: column;
  margin: var(--k-space-2);
  overflow: hidden;
  padding: 0;
  text-align: start;
  transition: border-color var(--k-motion-standard) var(--k-ease-standard), box-shadow var(--k-motion-standard) var(--k-ease-standard), transform var(--k-motion-standard) var(--k-ease-standard);
}

.series-directory-card:hover {
  border-color: color-mix(in srgb, var(--k-primary) 38%, transparent);
  box-shadow: 0 20px 52px rgba(3, 10, 30, 0.34), 0 0 24px color-mix(in srgb, var(--k-primary) 12%, transparent);
  transform: translateY(-5px);
}

.series-directory-card__visual {
  background: var(--k-surface-muted);
  display: block;
  aspect-ratio: var(--k-cover-aspect-ratio);
  overflow: hidden;
  position: relative;
  width: 100%;
}

.series-directory-card__cover,
.series-directory-card__placeholder {
  height: 100%;
  width: 100%;
}

.series-directory-card__placeholder {
  background: linear-gradient(145deg, color-mix(in srgb, var(--k-primary) 18%, var(--k-surface-muted)), var(--k-surface-muted));
  display: block;
}

.series-directory-card__body {
  display: grid;
  gap: var(--k-space-1);
  min-width: 0;
  min-height: 5rem;
  padding: var(--k-space-3) var(--k-space-2) var(--k-space-2);
}

.series-directory-card__body strong {
  display: -webkit-box;
  font-family: var(--k-font-display);
  font-weight: 700;
  line-height: 1.25;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.series-directory-card__body span {
  color: var(--k-text-secondary);
  font-size: var(--k-font-size-meta);
}

@media (max-width: 37.5rem) {
  .series-directories__grid { padding-inline: 0; }

  .series-directory-card { margin-inline: 6px; }
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
