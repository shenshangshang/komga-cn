<template>
  <v-toolbar flat
             :color="color"
             class="sticky-bar"
             :style="barStyle"
             :elevation="elevation"
  >
    <slot/>
  </v-toolbar>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
  name: 'ToolbarSticky',
  computed: {
    barStyle (): any {
      if (['xs', 'sm'].includes(this.$vuetify.breakpoint.name.toString())) {
        return { 'top': '56px' }
      } else {
        return { 'top': '64px' }
      }
    },
  },
  props: {
    elevation: {
      type: Number,
      default: undefined,
    },
    color: {
      type: String,
      default: 'contrast-1',
    },
  },
})
</script>

<style scoped>
.sticky-bar {
  position: -webkit-sticky;
  position: sticky;
  z-index: 2;
  margin: 8px var(--k-content-gutter);
  width: auto;
  border: 1px solid color-mix(in srgb, var(--k-primary) 14%, transparent);
  border-radius: 16px;
  background: color-mix(in srgb, var(--k-surface-card) 88%, transparent) !important;
  box-shadow: 0 12px 34px rgba(3, 10, 30, .2) !important;
  backdrop-filter: blur(16px) saturate(135%);
  transition: box-shadow var(--k-motion-fast) var(--k-ease-standard);
}

.sticky-bar ::v-deep .v-btn { min-width: 44px; min-height: 44px; }

@media (max-width: 600px) {
  .sticky-bar { margin-inline: 8px; border-radius: 14px; }
}
</style>
