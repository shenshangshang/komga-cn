<template>
  <label class="drop-zone" v-cloak @drop.prevent="dropHandler" @dragover.prevent>
    <span class="file-input">{{ $t('common.choose_image') }}</span> - {{ $t('common.drag_drop') }}
    <input ref="input" class="drop-zone__input" :aria-label="$t('common.choose_image')" type="file" accept="image/*" multiple @change="dropHandler">
  </label>
</template>

<script lang="ts">
import Vue from 'vue'
import {getFileFromUrl} from '@/functions/file'

export default Vue.extend({
  name: 'DropZone',
  methods: {
    async dropHandler(event: Event) {
      if (event instanceof DragEvent && event.dataTransfer) {
        if (event.dataTransfer.files.length > 0)
          this.$emit('on-input-change', Array.from(event.dataTransfer.files))
        else {
          const url = event.dataTransfer.getData('text/uri-list')
          if (url) {
            const file = await getFileFromUrl(url)
            this.$emit('on-input-change', [file])
          }
        }
      }
      if (event.target instanceof HTMLInputElement && event.target.files) {
        const selectedFiles = event.target.files
        if (!selectedFiles) return

        this.$emit('on-input-change', Array.from(selectedFiles))
      }
    },
    reset() {
      (this.$refs.input as HTMLInputElement).value = ''
    },
  },
})
</script>

<style scoped>
.drop-zone {
  position: relative;
  display: block;
  min-height: 7rem;
  padding: var(--k-space-8) var(--k-space-4);
  border: 2px dashed var(--k-border);
  border-radius: var(--k-radius-card);
  background: var(--k-surface-muted);
  color: var(--k-text-secondary);
  font-weight: 600;
  text-align: center;
  width: 100%;
}

.file-input {
  color: var(--k-primary);
}

.file-input:hover {
  text-decoration: underline;
}

.drop-zone__input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

.drop-zone:focus-within {
  outline: var(--k-focus-width) solid var(--k-focus);
  outline-offset: var(--k-focus-offset);
}
</style>
