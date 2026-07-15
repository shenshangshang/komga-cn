<template>
  <v-container fluid class="pa-6 k-view-shell k-settings-page">
    <v-row class="aurora-admin-section">
      <v-col><h1 class="text-h5">{{ $t('users.users') }}</h1></v-col>
    </v-row>
    <v-row class="aurora-admin-section mt-6">
      <v-col cols="12" md="10" lg="8" xl="4">
        <users-list/>
      </v-col>
    </v-row>

    <v-row>
      <v-col class="text-h5">{{ $t('users.authentication_activity') }}</v-col>
    </v-row>
    <v-row>
      <v-col cols="12">
        <authentication-activity-table/>
      </v-col>
    </v-row>
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
.aurora-admin-section {
  margin-left: 0;
  margin-right: 0;
  padding: 12px;
  border: 1px solid rgba(105, 212, 255, .14);
  border-radius: 22px;
  background: rgba(18, 31, 57, .82);
  box-shadow: 0 20px 56px rgba(2, 8, 23, .18);
}
</style>
