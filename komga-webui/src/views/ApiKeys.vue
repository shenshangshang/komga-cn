<template>
  <v-container fluid class="pa-6 k-view-shell k-settings-page">
    <v-row class="aurora-table-panel">
      <v-col cols="12" md="10" lg="8" xl="4">
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
.aurora-table-panel {
  margin: 0;
  padding: 12px;
  border: 1px solid rgba(105, 212, 255, .14);
  border-radius: 22px;
  background: rgba(18, 31, 57, .82);
}
</style>
