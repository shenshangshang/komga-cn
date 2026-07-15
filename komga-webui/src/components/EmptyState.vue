<template>
  <div class="empty-state-wrap text-center">
    <v-sheet class="empty-state" color="contrast-1" :style="{maxWidth: cssSize}" role="status" aria-live="polite">
      <div>
        <v-icon v-if="icon !== ''"
                :color="iconColor"
                :size="iconSize"
        >{{ icon }}
        </v-icon>
        <h2 class="text-h5">{{ title }}</h2>
        <p class="text-body-1">{{ subTitle }}</p>
        <slot/>
      </div>
    </v-sheet>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
  name: 'EmptyState',
  props: {
    title: String,
    subTitle: String,
    icon: String,
    iconColor: String,
    iconSize: {
      type: [Number, String],
      default: 140,
    },
    size: {
      type: [Number, String],
      default: 400,
    },
  },
  computed: {
    cssSize(): string {
      return typeof this.size === 'number' ? `${this.size}px` : this.size
    },
  },
})
</script>

<style scoped>
.empty-state-wrap {
  display: flex;
  justify-content: center;
  padding: var(--k-space-8) var(--k-content-gutter);
}

.empty-state {
  width: 100%;
  padding: var(--k-space-8);
  border: 1px solid color-mix(in srgb, var(--k-primary) 16%, transparent);
  border-radius: var(--k-radius-sheet) !important;
  background: linear-gradient(145deg, color-mix(in srgb, var(--k-primary) 7%, var(--k-surface-card)), var(--k-surface-card)) !important;
  box-shadow: 0 18px 48px rgba(3, 10, 30, .18);
}
</style>
