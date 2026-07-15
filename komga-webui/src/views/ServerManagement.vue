<template>
  <v-container fluid class="pa-6 k-view-shell k-settings-page">
    <div class="aurora-danger-panel">
    <v-row>
      <v-col><h2 class="text-h5">{{ $t('server.server_management.section_title') }}</h2></v-col>
    </v-row>
    <v-row>
      <v-col cols="auto">
        <v-btn @click="downloadLogFile"
        >{{ $t('server.server_management.download_log') }}
        </v-btn>
      </v-col>
    </v-row>
    </div>
    <v-row>
      <v-col cols="auto">
        <v-btn @click="cancelAllTasks"
               color="warning"
        >{{ $t('server.server_management.button_cancel_all_tasks') }}
        </v-btn>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="auto">
        <v-btn @click="modalStopServer = true"
               color="error"
        >{{ $t('server.server_management.button_shutdown') }}
        </v-btn>
      </v-col>
    </v-row>

    <confirmation-dialog
      v-model="modalStopServer"
      :title="$t('dialog.server_stop.dialog_title')"
      :body="$t('dialog.server_stop.confirmation_message')"
      :button-confirm="$t('dialog.server_stop.button_confirm')"
      button-confirm-color="error"
      @confirm="stopServer"
    />

  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import ConfirmationDialog from '@/components/dialogs/ConfirmationDialog.vue'
import {ERROR, ErrorEvent, NOTIFICATION, NotificationEvent} from '@/types/events'
import jsFileDownloader from 'js-file-downloader'
import urls from '@/functions/urls'

export default Vue.extend({
  name: 'ServerManagement',
  components: {ConfirmationDialog},
  data: () => ({
    modalStopServer: false,
  }),
  methods: {
    async cancelAllTasks() {
      const count = await this.$komgaTasks.deleteAllTasks()
      this.$eventHub.$emit(NOTIFICATION, {
        message: this.$tc('server.server_management.notification_tasks_cancelled', count),
      } as NotificationEvent)
    },
    async stopServer() {
      try {
        await this.$actuator.shutdown()
      } catch (e) {
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
      }
    },
    downloadLogFile() {
      new jsFileDownloader({
        url: `${urls.originNoSlash}${this.$actuator.logfileUrl()}`,
        filename: 'komga.log',
        withCredentials: true,
        forceDesktopMode: true,
      })
    },
  },
})
</script>

<style scoped>
.aurora-danger-panel {
  max-width: 760px;
  padding: 24px;
  border: 1px solid rgba(255, 112, 151, .22);
  border-radius: 22px;
  background: linear-gradient(145deg, rgba(48, 31, 57, .9), rgba(17, 26, 49, .96));
}

.aurora-danger-panel ::v-deep .v-btn {
  min-height: 44px;
  border-radius: 14px;
}
</style>
