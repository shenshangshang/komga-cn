<template>
  <v-container fluid class="pa-6 k-view-shell k-settings-page">
    <v-row>
      <v-col><h1 class="text-h5">{{ $t('server_settings.server_settings') }}</h1></v-col>
    </v-row>
    <v-row>
      <v-col cols="12" md="9" lg="7" class="k-settings-surface server-settings-panel">
        <v-select
          v-model="form.registrationMode"
          @change="$v.form.registrationMode.$touch()"
          :items="registrationModes"
          label="用户注册模式"
          hint="关闭、开放注册或仅邀请注册"
          persistent-hint
        />
        <div v-if="form.registrationMode === 'INVITE'" class="mt-4 pa-4">
          <div class="text-subtitle-1 mb-2">邀请链接</div>
          <v-text-field v-model.number="invitationExpiresInDays" label="有效天数" type="number" min="1" max="30" />
          <v-btn color="primary" @click="createInvitation">生成邀请链接</v-btn>
          <v-text-field v-if="createdInvitationUrl" v-model="createdInvitationUrl" readonly class="mt-3" label="仅显示一次，请立即复制" />
          <v-list dense v-if="invitations.length">
            <v-list-item v-for="invitation in invitations" :key="invitation.id">
              <v-list-item-content>
                <v-list-item-title>{{ invitation.id }}</v-list-item-title>
                <v-list-item-subtitle>到期：{{ invitation.expiresDate }} · {{ invitation.usedDate ? '已使用' : invitation.revokedDate ? '已撤销' : '可用' }}</v-list-item-subtitle>
              </v-list-item-content>
              <v-list-item-action v-if="!invitation.usedDate && !invitation.revokedDate">
                <v-btn icon @click="revokeInvitation(invitation.id)"><v-icon>mdi-delete</v-icon></v-btn>
              </v-list-item-action>
            </v-list-item>
          </v-list>
        </div>
        <v-select
          v-model="form.thumbnailSize"
          @change="$v.form.thumbnailSize.$touch()"
          :items="thumbnailSizes"
          :label="$t('server_settings.label_thumbnail_size')"
          hide-details
        />
        <v-checkbox
          v-model="form.deleteEmptyCollections"
          @change="$v.form.deleteEmptyCollections.$touch()"
          :label="$t('server_settings.label_delete_empty_collections')"
          hide-details
        />
        <v-checkbox
          v-model="form.deleteEmptyReadLists"
          @change="$v.form.deleteEmptyReadLists.$touch()"
          :label="$t('server_settings.label_delete_empty_readlists')"
          hide-details
        />
        <v-text-field
          v-model="form.taskPoolSize"
          @input="$v.form.taskPoolSize.$touch()"
          @blur="$v.form.taskPoolSize.$touch()"
          :error-messages="taskPoolSizeErrors"
          :label="$t('server_settings.label_task_pool_size')"
          type="number"
          min="1"
          class="mt-4"
        />
        <v-text-field
          v-model="form.prefetchPages"
          @input="$v.form.prefetchPages.$touch()"
          @blur="$v.form.prefetchPages.$touch()"
          :error-messages="prefetchPagesErrors"
          :label="$t('server_settings.label_prefetch_pages')"
          :hint="$t('server_settings.hint_prefetch_pages')"
          persistent-hint
          type="number"
          min="0"
          max="10"
          class="mt-4"
        />
        <v-text-field
          v-model="form.rememberMeDurationDays"
          @input="$v.form.rememberMeDurationDays.$touch()"
          @blur="$v.form.rememberMeDurationDays.$touch()"
          :error-messages="rememberMeDurationErrors"
          :label="$t('server_settings.label_rememberme_duration')"
          :hint="$t('server_settings.requires_restart')"
          persistent-hint
          type="number"
          min="1"
          class="mt-4"
        />
        <v-checkbox
          v-model="form.renewRememberMeKey"
          @change="$v.form.renewRememberMeKey.$touch()"
          label="重新生成“记住我”密钥"
          persistent-hint
          :hint="$t('server_settings.requires_restart')"
        />

        <v-text-field
          v-model="form.serverPort"
          @input="$v.form.serverPort.$touch()"
          @blur="$v.form.serverPort.$touch()"
          :error-messages="serverPortErrors"
          :placeholder="existingSettings.serverPort?.configurationSource?.toString()"
          :persistent-placeholder="!!existingSettings.serverPort?.configurationSource"
          :hint="$t('server_settings.requires_restart')"
          persistent-hint
          clearable
          :label="$t('server_settings.label_server_port')"
          type="number"
          min="1"
          max="65535"
          class="mt-4"
        >
          <template v-slot:append v-if="!!existingSettings.serverPort?.configurationSource">
            <v-tooltip bottom>
              <template v-slot:activator="{ on }">
                <v-icon v-on="on">
                  mdi-information-outline
                </v-icon>
              </template>
              {{ $t('server_settings.config_precedence') }}
            </v-tooltip>
          </template>
        </v-text-field>

        <v-text-field
          v-model="form.serverContextPath"
          @input="$v.form.serverContextPath.$touch()"
          @blur="$v.form.serverContextPath.$touch()"
          :error-messages="serverContextPathErrors"
          :placeholder="existingSettings.serverContextPath?.configurationSource"
          :persistent-placeholder="!!existingSettings.serverContextPath?.configurationSource"
          :hint="$t('server_settings.requires_restart')"
          persistent-hint
          clearable
          :label="$t('server_settings.label_server_context_path')"
          class="mt-4"
        >
          <template v-slot:append v-if="!!existingSettings.serverContextPath?.configurationSource">
            <v-tooltip bottom>
              <template v-slot:activator="{ on }">
                <v-icon v-on="on">
                  mdi-information-outline
                </v-icon>
              </template>
              {{ $t('server_settings.config_precedence') }}
            </v-tooltip>
          </template>
        </v-text-field>

        <v-checkbox
          v-model="form.koboProxy"
          @change="$v.form.koboProxy.$touch()"
          :label="$t('server_settings.label_kobo_proxy')"
          hide-details
        />

        <v-text-field
          v-model="form.koboPort"
          @input="$v.form.koboPort.$touch()"
          @blur="$v.form.koboPort.$touch()"
          :error-messages="koboPortErrors"
          clearable
          :label="$t('server_settings.label_kobo_port')"
          :hint="$t('server_settings.hint_kobo_port')"
          persistent-hint
          type="number"
          min="1"
          max="65535"
          class="mt-4"
        />

        <file-browser-dialog
          v-model="modalFileBrowserKepubify"
          @confirm="$v.form.kepubifyPath.$touch()"
          :path.sync="form.kepubifyPath"
          :show-files="true"
        />

        <v-text-field
          v-model="form.kepubifyPath"
          @input="$v.form.kepubifyPath.$touch()"
          @blur="$v.form.kepubifyPath.$touch()"
          :error-messages="serverContextPathErrors"
          :placeholder="existingSettings.kepubifyPath?.configurationSource"
          :persistent-placeholder="!!existingSettings.kepubifyPath?.configurationSource"
          clearable
          :label="$t('server_settings.label_kepubify_path')"
          class="mt-4"
        >
          <template v-slot:append v-if="!!existingSettings.kepubifyPath?.configurationSource">
            <v-tooltip bottom>
              <template v-slot:activator="{ on }">
                <v-icon v-on="on">
                  mdi-information-outline
                </v-icon>
              </template>
              {{ $t('server_settings.config_precedence') }}
            </v-tooltip>
          </template>

          <template v-slot:append-outer>
            <v-btn small @click="modalFileBrowserKepubify = true">{{ $t('dialog.edit_library.button_browse') }}</v-btn>
          </template>
        </v-text-field>

      </v-col>
    </v-row>
    <v-row>
      <v-col cols="auto">
        <v-btn @click="refreshSettings"
               :disabled="discardDisabled"
        >{{ $t('common.discard') }}
        </v-btn>
      </v-col>
      <v-col cols="auto">
        <v-btn color="primary"
               :disabled="saveDisabled"
               @click="saveSettings"
        >{{ $t('common.save_changes') }}
        </v-btn>
      </v-col>
    </v-row>

    <confirmation-dialog
      v-model="dialogRegenerateThumbnails"
      :title="$t('server_settings.dialog_regenerate_thumbnails.title')"
      :body="$t('server_settings.dialog_regenerate_thumbnails.body')"
      :button-confirm="$t('server_settings.dialog_regenerate_thumbnails.btn_confirm')"
      :button-alternate="$t('server_settings.dialog_regenerate_thumbnails.btn_alternate')"
      :button-cancel="$t('server_settings.dialog_regenerate_thumbnails.btn_cancel')"
      @confirm="regenerateThumbnails(true)"
      @alternate="regenerateThumbnails(false)"
    />
  </v-container>
</template>

<script lang="ts">
import {RegistrationMode, SettingsDto, ThumbnailSizeDto} from '@/types/komga-settings'
import Vue from 'vue'
import {helpers, integer, maxValue, minValue, required} from 'vuelidate/lib/validators'
import ConfirmationDialog from '@/components/dialogs/ConfirmationDialog.vue'
import FileBrowserDialog from '@/components/dialogs/FileBrowserDialog.vue'

const contextPath = helpers.regex('contextPath', /^\/[-a-zA-Z0-9_\/]*[a-zA-Z0-9]$/)

export default Vue.extend({
  name: 'ServerSettings',
  components: {FileBrowserDialog, ConfirmationDialog},
  data: () => ({
    form: {
      registrationMode: RegistrationMode.DISABLED,
      deleteEmptyCollections: false,
      deleteEmptyReadLists: false,
      rememberMeDurationDays: 365,
      renewRememberMeKey: false,
      thumbnailSize: ThumbnailSizeDto.DEFAULT,
      taskPoolSize: 1,
      prefetchPages: 3,
      serverPort: 25600,
      serverContextPath: '',
      koboProxy: false,
      koboPort: undefined,
      kepubifyPath: undefined,
    },
    existingSettings: {} as SettingsDto,
    invitations: [] as any[],
    invitationExpiresInDays: 7,
    createdInvitationUrl: '',
    dialogRegenerateThumbnails: false,
    modalFileBrowserKepubify: false,
  }),
  validations: {
    form: {
      registrationMode: {},
      deleteEmptyCollections: {},
      deleteEmptyReadLists: {},
      rememberMeDurationDays: {
        minValue: minValue(1),
        required,
      },
      renewRememberMeKey: {},
      thumbnailSize: {},
      taskPoolSize: {
        minValue: minValue(1),
        required,
      },
      serverPort: {
        minValue: minValue(1),
        maxValue: maxValue(65535),
      },
      serverContextPath: {
        contextPath,
      },
      koboProxy: {},
      koboPort: {
        minValue: minValue(1),
        maxValue: maxValue(65535),
      },
      kepubifyPath: {},
      prefetchPages: {
        minValue: minValue(0),
        integer,
      },
    },
  },
  mounted() {
    this.refreshSettings()
  },
  computed: {
    registrationModes(): any[] {
      return [
        {text: '关闭注册', value: RegistrationMode.DISABLED},
        {text: '开放注册', value: RegistrationMode.OPEN},
        {text: '仅邀请注册', value: RegistrationMode.INVITE},
      ]
    },
    thumbnailSizes(): any[] {
      return Object.keys(ThumbnailSizeDto).map(x => ({
        text: this.$t(`enums.thumbnail_size.${x}`),
        value: x,
      }))
    },
    rememberMeDurationErrors(): string[] {
      const errors = [] as string[]
      if (!this.$v.form?.rememberMeDurationDays?.$dirty) return errors
      !this.$v?.form?.rememberMeDurationDays?.minValue && errors.push(this.$t('validation.one_or_more').toString())
      !this.$v?.form?.rememberMeDurationDays?.required && errors.push(this.$t('common.required').toString())
      return errors
    },
    taskPoolSizeErrors(): string[] {
      const errors = [] as string[]
      if (!this.$v.form?.taskPoolSize?.$dirty) return errors
      !this.$v?.form?.taskPoolSize?.minValue && errors.push(this.$t('validation.one_or_more').toString())
      !this.$v?.form?.taskPoolSize?.required && errors.push(this.$t('common.required').toString())
      return errors
    },
    prefetchPagesErrors(): string[] {
      const errors = [] as string[]
      if (!this.$v.form?.prefetchPages?.$dirty) return errors
      !this.$v?.form?.prefetchPages?.minValue && errors.push(this.$t('validation.min_value', {value: 0}).toString())
      !this.$v?.form?.prefetchPages?.integer && errors.push(this.$t('validation.integer').toString())
      return errors
    },
    serverPortErrors(): string[] {
      const errors = [] as string[]
      if (!this.$v.form?.serverPort?.$dirty) return errors;
      (!this.$v?.form?.serverPort?.minValue || !this.$v?.form?.serverPort?.maxValue) && errors.push(this.$t('validation.tcp_port').toString())
      return errors
    },
    serverContextPathErrors(): string[] {
      const errors = [] as string[]
      if (!this.$v.form?.serverContextPath?.$dirty) return errors
      !this.$v?.form?.serverContextPath?.contextPath && errors.push(this.$t('validation.context_path').toString())
      return errors
    },
    koboPortErrors(): string[] {
      const errors = [] as string[]
      if (!this.$v.form?.koboPort?.$dirty) return errors;
      (!this.$v?.form?.koboPort?.minValue || !this.$v?.form?.koboPort?.maxValue) && errors.push(this.$t('validation.tcp_port').toString())
      return errors
    },
    saveDisabled(): boolean {
      return this.$v.form.$invalid || !this.$v.form.$anyDirty
    },
    discardDisabled(): boolean {
      return !this.$v.form.$anyDirty
    },
  },
  methods: {
    async refreshInvitations() {
      this.invitations = (await this.$http.get('/api/v1/invitations')).data
    },
    async createInvitation() {
      const invitation = (await this.$http.post('/api/v1/invitations', {expiresInDays: this.invitationExpiresInDays})).data
      const href = this.$router.resolve({name: 'register', query: {token: invitation.token}}).href
      this.createdInvitationUrl = `${window.location.origin}${href}`
      await this.refreshInvitations()
    },
    async revokeInvitation(id: string) {
      await this.$http.delete(`/api/v1/invitations/${id}`)
      await this.refreshInvitations()
    },
    async refreshSettings() {
      const settings = await (this.$komgaSettings.getSettings())
      this.$_.merge(this.form, settings)
      this.form.serverPort = settings.serverPort.databaseSource
      this.form.serverContextPath = settings.serverContextPath.databaseSource
      this.form.kepubifyPath = settings.kepubifyPath.databaseSource
      this.$_.merge(this.existingSettings, settings)
      this.$v.form.$reset()
      if (settings.registrationMode === RegistrationMode.INVITE) await this.refreshInvitations()
    },
    async saveSettings() {
      const newSettings = {}
      if (this.$v.form?.registrationMode?.$dirty)
        this.$_.merge(newSettings, {registrationMode: this.form.registrationMode})
      let thumbnailSizeHasChanged = false
      if (this.$v.form?.deleteEmptyCollections?.$dirty)
        this.$_.merge(newSettings, {deleteEmptyCollections: this.form.deleteEmptyCollections})
      if (this.$v.form?.deleteEmptyReadLists?.$dirty)
        this.$_.merge(newSettings, {deleteEmptyReadLists: this.form.deleteEmptyReadLists})
      if (this.$v.form?.rememberMeDurationDays?.$dirty)
        this.$_.merge(newSettings, {rememberMeDurationDays: this.form.rememberMeDurationDays})
      if (this.$v.form?.renewRememberMeKey?.$dirty)
        this.$_.merge(newSettings, {renewRememberMeKey: this.form.renewRememberMeKey})
      if (this.$v.form?.thumbnailSize?.$dirty) {
        this.$_.merge(newSettings, {thumbnailSize: this.form.thumbnailSize})
        thumbnailSizeHasChanged = this.existingSettings.thumbnailSize != this.form.thumbnailSize
      }
      if (this.$v.form?.taskPoolSize?.$dirty)
        this.$_.merge(newSettings, {taskPoolSize: this.form.taskPoolSize})
      if (this.$v.form?.prefetchPages?.$dirty)
        this.$_.merge(newSettings, {prefetchPages: this.form.prefetchPages})
      if (this.$v.form?.serverPort?.$dirty)
        this.$_.merge(newSettings, {serverPort: this.form.serverPort})
      if (this.$v.form?.serverContextPath?.$dirty)
        // coerce empty string to null
        this.$_.merge(newSettings, {serverContextPath: this.form.serverContextPath || null})

      if (this.$v.form?.koboProxy?.$dirty)
        this.$_.merge(newSettings, {koboProxy: this.form.koboProxy})
      if (this.$v.form?.koboPort?.$dirty)
        this.$_.merge(newSettings, {koboPort: this.form.koboPort})
      if (this.$v.form?.kepubifyPath?.$dirty)
        this.$_.merge(newSettings, {kepubifyPath: this.form.kepubifyPath})


      await this.$komgaSettings.updateSettings(newSettings)
      await this.refreshSettings()

      if (thumbnailSizeHasChanged) {
        this.dialogRegenerateThumbnails = true
      }
    },
    regenerateThumbnails(forBiggerResultOnly: boolean) {
      this.$komgaBooks.regenerateThumbnails(forBiggerResultOnly)
    },
  },
})
</script>

<style scoped>
.server-settings-panel ::v-deep .v-input:not(:last-child) {
  margin-bottom: 8px;
}
</style>
