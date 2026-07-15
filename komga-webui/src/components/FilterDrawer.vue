<template>
  <v-navigation-drawer
    v-model="display"
    :right="!$vuetify.rtl"
    fixed
    temporary
    disable-route-watcher
    class="fill-height filter-drawer"
  >
    <v-fab-transition>
      <v-btn
        v-if="clearButton"
        absolute
        top
        right
        icon
        class="k-touch-target filter-drawer__clear"
        :aria-label="$t('common.reset_filters')"
        elevation="2"
        @click="clear"
        color="primary"
      >
        <v-icon>mdi-filter-variant-remove</v-icon>
      </v-btn>
    </v-fab-transition>

    <slot></slot>

    <template v-if="$slots.filter">
      <v-divider/>
      <v-subheader class="text-uppercase">{{ $t('filter_drawer.filter') }}</v-subheader>
    </template>
    <slot name="filter"></slot>

    <template v-if="$slots.sort">
      <v-divider/>
      <v-subheader class="text-uppercase">{{ $t('filter_drawer.sort') }}</v-subheader>
    </template>
    <slot name="sort"></slot>

  </v-navigation-drawer>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
  name: 'FilterDrawer',
  data: () => {
    return {
      display: false,
    }
  },
  props: {
    value: Boolean,
    clearButton: {
      type: Boolean,
      default: false,
    },
  },
  watch: {
    value(val) {
      this.display = val
    },
    display(val) {
      !val && this.$emit('input', false)
    },
  },
  methods: {
    clear() {
      this.$emit('clear')
    },
  },
})
</script>

<style scoped>
.filter-drawer__clear {
  z-index: var(--k-z-sticky);
  background: var(--k-surface-card);
}

.filter-drawer {
  border-inline-start: 1px solid color-mix(in srgb, var(--k-primary) 18%, transparent);
  background: color-mix(in srgb, var(--k-surface-card) 91%, transparent) !important;
  box-shadow: -18px 0 54px rgba(3, 10, 30, .38) !important;
  backdrop-filter: blur(20px) saturate(140%);
}

.filter-drawer ::v-deep .v-subheader {
  color: var(--k-primary);
  font-size: .75rem;
  font-weight: 700;
  letter-spacing: .12em;
}

.filter-drawer ::v-deep .v-list-item,
.filter-drawer ::v-deep .v-btn { min-height: 44px; }

.filter-drawer ::v-deep .v-divider {
  border-color: color-mix(in srgb, var(--k-primary) 12%, transparent);
}
</style>
