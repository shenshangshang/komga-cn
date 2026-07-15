<template>
  <v-container fluid class="pa-6 k-view-shell k-settings-page">
    <v-row class="k-settings-surface k-settings-surface--compact api-keys-panel">
      <v-col cols="12">
        <api-key-table/>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import ApiKeyTable from '@/components/ApiKeyTable.vue'

export default Vue.extend({
  name: 'ApiKeys',
  components: {ApiKeyTable},
})
</script>

<style scoped>
.api-keys-panel {
  margin-inline: 0;
}
</style>
