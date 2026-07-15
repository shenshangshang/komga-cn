<template>
  <v-container fluid class="pa-6 k-view-shell k-settings-page">
    <section class="k-settings-surface settings-users-section">
      <v-row>
        <v-col><h1 class="text-h5">{{ $t('users.users') }}</h1></v-col>
      </v-row>
      <v-row>
        <v-col cols="12" md="10" lg="8">
          <users-list/>
        </v-col>
      </v-row>
    </section>

    <section class="k-settings-surface settings-users-section mt-6">
      <v-row>
        <v-col><h2 class="text-h5">{{ $t('users.authentication_activity') }}</h2></v-col>
      </v-row>
      <v-row>
        <v-col cols="12">
          <authentication-activity-table/>
        </v-col>
      </v-row>
    </section>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import AuthenticationActivityTable from '@/components/AuthenticationActivityTable.vue'
import UsersList from '@/components/UsersList.vue'

export default Vue.extend({
  name: 'SettingsUsers',
  components: {UsersList, AuthenticationActivityTable  },
})
</script>

<style scoped>
.settings-users-section {
  margin-inline: auto;
}
</style>
