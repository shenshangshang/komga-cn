<template>
  <button
    type="button"
    class="series-directory-card"
    :style="{width: `${width}px`}"
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
</template>

<script lang="ts">
import Vue from 'vue'
import {bookThumbnailUrl} from '@/functions/urls'
import {SeriesDirectoryDto} from '@/types/komga-directories'

export default Vue.extend({
  name: 'SeriesDirectoryCard',
  props: {
    directory: {
      type: Object as () => SeriesDirectoryDto,
      required: true,
    },
    width: {
      type: Number,
      required: true,
    },
  },
  methods: {bookThumbnailUrl},
})
</script>

<style scoped>
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

.series-directory-card:focus-visible {
  outline: var(--k-focus-width) solid var(--k-focus);
  outline-offset: var(--k-focus-offset);
}

.series-directory-card__visual {
  aspect-ratio: var(--k-cover-aspect-ratio);
  background: var(--k-surface-muted);
  display: block;
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
  min-height: 5rem;
  min-width: 0;
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
  .series-directory-card { margin-inline: 6px; }
}

@media (prefers-reduced-motion: reduce) {
  .series-directory-card { transition: none; }
  .series-directory-card:hover { transform: none; }
}
</style>
