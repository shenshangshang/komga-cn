<template>
  <v-container fluid class="pa-6">
    <h1 class="text-h5 mb-4">上传漫画压缩包</h1>
    <v-card class="pa-6" max-width="720">
      <v-autocomplete
        v-model="libraryId"
        :items="libraries"
        item-text="name"
        item-value="id"
        label="目标媒体库"
        :loading="loadingLibraries"
      />
      <v-radio-group v-model="targetMode" row :disabled="!libraryId">
        <v-radio label="添加到现有系列" value="existing" />
        <v-radio label="新建系列" value="new" />
      </v-radio-group>
      <v-autocomplete
        v-if="targetMode === 'existing'"
        v-model="seriesId"
        :items="series"
        item-text="name"
        item-value="id"
        label="目标系列"
        :loading="loadingSeries"
        :disabled="!libraryId"
        no-data-text="此媒体库没有可上传的系列"
      />
      <v-text-field
        v-else
        v-model="newSeriesName"
        label="新系列名称"
        hint="将在所选媒体库根目录下创建同名系列目录"
        persistent-hint
        :disabled="!libraryId"
        maxlength="200"
      />
      <v-file-input v-model="file" accept=".cbz,.cbr,.zip,.rar,.7z" label="漫画压缩包（最大 1 GiB）" />
      <v-alert v-if="!loadingLibraries && libraries.length === 0" type="warning" text>当前账号没有可用的媒体库</v-alert>
      <v-alert v-if="error" type="error" text>{{ error }}</v-alert>
      <v-alert v-if="success" type="success" text>上传并导入完成</v-alert>
      <v-btn color="primary" :disabled="!canUpload" :loading="uploading" @click="upload">上传</v-btn>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import {LibraryDto} from '@/types/komga-libraries'
import {SeriesDto} from '@/types/komga-series'
import {SearchConditionLibraryId, SearchOperatorIs} from '@/types/komga-search'

export default Vue.extend({
  name: 'UploadBooks',
  data: () => ({
    libraries: [] as LibraryDto[],
    libraryId: '',
    series: [] as SeriesDto[],
    seriesId: '',
    newSeriesName: '',
    targetMode: 'existing' as 'existing' | 'new',
    file: null as File | null,
    loadingLibraries: false,
    loadingSeries: false,
    uploading: false,
    error: '',
    success: false,
  }),
  computed: {
    canUpload(): boolean {
      if (!this.libraryId || !this.file) return false
      return this.targetMode === 'existing' ? !!this.seriesId : !!this.newSeriesName.trim()
    },
  },
  watch: {
    libraryId() {
      this.seriesId = ''
      this.loadSeries()
    },
    targetMode() {
      this.error = ''
      this.success = false
    },
  },
  async mounted() {
    this.loadingLibraries = true
    try {
      this.libraries = await this.$komgaLibraries.getLibraries()
      if (this.libraries.length === 1) this.libraryId = this.libraries[0].id
    } catch (e) {
      this.error = e.message || '加载媒体库失败'
    } finally {
      this.loadingLibraries = false
    }
  },
  methods: {
    async loadSeries() {
      this.series = []
      if (!this.libraryId) return
      this.loadingSeries = true
      try {
        this.series = (
          await this.$komgaSeries.getSeriesList({
            condition: new SearchConditionLibraryId(new SearchOperatorIs(this.libraryId)),
          }, {unpaged: true})
        ).content.filter(x => !x.oneshot)
      } catch (e) {
        this.error = e.message || '加载系列失败'
      } finally {
        this.loadingSeries = false
      }
    },
    async upload() {
      if (!this.canUpload) return
      this.uploading = true
      this.error = ''
      this.success = false
      try {
        await this.$komgaBooks.uploadBook({
          libraryId: this.libraryId,
          seriesId: this.targetMode === 'existing' ? this.seriesId : undefined,
          seriesName: this.targetMode === 'new' ? this.newSeriesName.trim() : undefined,
        }, this.file!)
        this.file = null
        this.success = true
        if (this.targetMode === 'new') {
          this.newSeriesName = ''
          await this.loadSeries()
        }
      } catch (e) {
        this.error = e.response?.data?.message || e.message || '上传失败'
      } finally {
        this.uploading = false
      }
    },
  },
})
</script>
