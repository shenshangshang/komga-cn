<template>
  <v-container fluid class="pa-6 k-view-shell k-settings-page aurora-server-stack">
    <v-row>
      <v-col>
        <server-settings/>
      </v-col>
    </v-row>
    <v-row>
      <v-col>
        <server-management/>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import ServerSettings from '@/views/ServerSettings.vue'
import ServerManagement from '@/views/ServerManagement.vue'

export default Vue.extend({
  name: 'SettingsServer',
  components: {ServerManagement, ServerSettings},
})
</script>

<style scoped>
.aurora-server-stack {
  max-width: 1160px;
}

.aurora-server-stack > .row {
  margin-bottom: 18px;
}
</style>
