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
      v-if="showCards && directories.length"
      ref="content"
      v-resize="onResize"
      class="series-directories__grid"
    >
      <series-directory-card
        v-for="directory in directories"
        :key="directory.path"
        :directory="directory"
        :width="cardWidth"
        @navigate="$emit('navigate', $event)"
      />
    </div>
  </section>
</template>

<script lang="ts">
import Vue from 'vue'
import {SeriesDirectoryBreadcrumbDto, SeriesDirectoryDto} from '@/types/komga-directories'
import {computeCardWidth} from '@/functions/grid-utilities'
import SeriesDirectoryCard from '@/components/SeriesDirectoryCard.vue'

export default Vue.extend({
  name: 'SeriesDirectoryBrowser',
  components: {SeriesDirectoryCard},
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
    showCards: {
      type: Boolean,
      default: true,
    },
  },
  methods: {
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
.series-directories__crumb:focus-visible {
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

@media (max-width: 37.5rem) {
  .series-directories__grid { padding-inline: 0; }

}
</style>
