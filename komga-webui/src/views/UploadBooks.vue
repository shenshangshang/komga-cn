<template>
  <v-container fluid class="pa-6">
    <h1 class="text-h5 mb-4">上传漫画压缩包</h1>
    <v-card class="pa-6" max-width="720">
      <v-autocomplete v-model="seriesId" :items="series" item-text="name" item-value="id" label="目标系列" :loading="loadingSeries" />
      <v-file-input v-model="file" accept=".cbz,.cbr,.zip,.rar,.7z" label="漫画压缩包（最大 1 GiB）" />
      <v-alert v-if="error" type="error" text>{{ error }}</v-alert>
      <v-alert v-if="success" type="success" text>上传并导入完成</v-alert>
      <v-btn color="primary" :disabled="!seriesId || !file" :loading="uploading" @click="upload">上传</v-btn>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import {SeriesDto} from '@/types/komga-series'

export default Vue.extend({
  name: 'UploadBooks',
  data: () => ({
    series: [] as SeriesDto[],
    seriesId: '',
    file: null as File | null,
    loadingSeries: false,
    uploading: false,
    error: '',
    success: false,
  }),
  async mounted() {
    this.loadingSeries = true
    try {
      this.series = (await this.$komgaSeries.getSeriesList({}, {size: 1000})).content.filter(x => !x.oneshot)
    } finally {
      this.loadingSeries = false
    }
  },
  methods: {
    async upload() {
      if (!this.file || !this.seriesId) return
      this.uploading = true
      this.error = ''
      this.success = false
      try {
        await this.$komgaBooks.uploadBook(this.seriesId, this.file)
        this.file = null
        this.success = true
      } catch (e) {
        this.error = e.response?.data?.message || e.message || '上传失败'
      } finally {
        this.uploading = false
      }
    },
  },
})
</script>
