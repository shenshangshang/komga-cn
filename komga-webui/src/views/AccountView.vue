<template>
  <v-container fluid class="pa-6 k-view-shell k-settings-page">
    <div class="aurora-settings-panel">
    <v-row align="center">
      <v-col cols="12" md="8" lg="6" xl="4">
        <span class="text-capitalize">{{ $t('common.email') }}</span>
        <v-text-field readonly
                      v-model="me.email"
        />
      </v-col>
    </v-row>

    <v-row align="center">
      <v-col>
        <span>{{ $t('common.roles') }}</span>
        <v-chip-group>
          <v-chip v-for="role in me.roles" :key="role"
          >{{ $t(`user_roles.${role}`) }}
          </v-chip>
          <v-chip v-if="me.roles.length === 0">USER</v-chip>
        </v-chip-group>
      </v-col>
    </v-row>

    <v-row>
      <v-col>
        <v-btn color="primary"
               @click.prevent="modalPasswordChange = true"
        >{{ $t('account_settings.change_password') }}
        </v-btn>
      </v-col>
    </v-row>
    </div>

    <password-change-dialog v-model="modalPasswordChange"
                            :user="me"
    />

  </v-container>
</template>

<script lang="ts">
import PasswordChangeDialog from '@/components/dialogs/PasswordChangeDialog.vue'
import Vue from 'vue'
import {UserDto} from '@/types/komga-users'

export default Vue.extend({
  name: 'AccountSettings',
  components: {PasswordChangeDialog},
  data: () => {
    return {
      modalPasswordChange: false,
      newPassword: '',
    }
  },
  computed: {
    me(): UserDto {
      return this.$store.state.komgaUsers.me
    },
  },
})
</script>

<style scoped>
.aurora-settings-panel {
  max-width: 760px;
  padding: clamp(20px, 4vw, 36px);
  border: 1px solid rgba(100, 210, 255, .16);
  border-radius: 24px;
  background: linear-gradient(145deg, rgba(24, 38, 67, .96), rgba(13, 23, 45, .96));
  box-shadow: 0 24px 70px rgba(2, 8, 23, .24);
}

.aurora-settings-panel ::v-deep .v-chip {
  border: 1px solid rgba(113, 226, 255, .2);
  background: rgba(80, 202, 238, .11) !important;
}
</style>
