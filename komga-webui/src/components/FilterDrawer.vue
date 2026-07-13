<template>
  <v-navigation-drawer
    v-model="display"
    :right="!$vuetify.rtl"
    fixed
    temporary
    disable-route-watcher
    class="fill-height"
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
</style>
