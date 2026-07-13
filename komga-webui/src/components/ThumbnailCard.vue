<template>
  <v-card class="thumbnail-card" outlined>
    <v-img
      :src="imageUrl"
      class="thumbnail-card__cover"
      aspect-ratio="0.7071"
      contain/>
    <v-card-text style="height: 5rem">
      <div>{{ fileSize }}</div>
      <div v-if="dimension">{{ $t('common.dimension', dimension) }}</div>
      <div v-if="mediaType">{{ mediaType }}</div>
    </v-card-text>
    <v-card-actions>
      <v-tooltip top>
        <template v-slot:activator="{ on, attrs }">
          <v-icon
            class="v-btn--icon v-size--default px-2"
            :color="fileTooBig ? 'error' : ''"
            role="img"
            :aria-label="statusTooltip"
            v-bind="attrs"
            v-on="on">
            {{ statusIcon }}
          </v-icon>
        </template>
        <span>{{ statusTooltip }}</span>
      </v-tooltip>

      <v-tooltip v-if="!fileTooBig" top>
        <template v-slot:activator="{ on, attrs }">
          <v-btn
            icon
            class="k-touch-target"
            :aria-label="selected ? $t('thumbnail_card.tooltip_selected') : $t('thumbnail_card.tooltip_mark_as_selected')"
            :color="selected ? 'success' : ''"
            @click="onClickSelect"
            v-bind="attrs"
            v-on="on">
            <v-icon>mdi-check</v-icon>
          </v-btn>
        </template>
        <span>{{
            selected ? $t('thumbnail_card.tooltip_selected') : $t('thumbnail_card.tooltip_mark_as_selected')
          }}</span>
      </v-tooltip>

      <v-tooltip v-if="deletable" top>
        <template v-slot:activator="{ on, attrs }">
          <v-btn
            icon
            class="k-touch-target"
            :aria-label="toBeDeleted ? $t('thumbnail_card.tooltip_to_be_deleted') : $t('thumbnail_card.tooltip_delete')"
            :color="toBeDeleted ? 'error' : ''"
            @click="onClickDelete"
            v-bind="attrs"
            v-on="on">
            <v-icon>mdi-trash-can-outline</v-icon>
          </v-btn>
        </template>
        <span>{{
            toBeDeleted ? $t('thumbnail_card.tooltip_to_be_deleted') : $t('thumbnail_card.tooltip_delete')
          }}</span>
      </v-tooltip>
    </v-card-actions>
  </v-card>
</template>

<script lang="ts">
import Vue, {PropType} from 'vue'
import {SeriesThumbnailDto} from '@/types/komga-series'
import {
  bookThumbnailUrlByThumbnailId,
  collectionThumbnailUrlByThumbnailId,
  readListThumbnailUrlByThumbnailId,
  seriesThumbnailUrlByThumbnailId,
} from '@/functions/urls'
import {BookThumbnailDto} from '@/types/komga-books'
import {ReadListThumbnailDto} from '@/types/komga-readlists'
import {Dimension} from '@/types/image'
import {getFileSize} from '@/functions/file'

export default Vue.extend({
  name: 'ThumbnailCard',
  data: () => ({
    objectUrl: undefined as string | undefined,
  }),
  props: {
    item: {
      required: true,
      validator: (value: any) => {
        if (value instanceof File) {
          return true
        }
        return 'id' in value && 'type' in value && 'selected' in value && ('seriesId' in value || 'bookId' in value || 'readListId' in value || 'collectionId' in value)
      },
    },
    selected: {
      type: Boolean,
      required: true,
    },
    toBeDeleted: {
      type: Boolean,
      required: true,
    },
  },
  created() {
    this.updateObjectUrl()
  },
  beforeDestroy() {
    this.revokeObjectUrl()
  },
  watch: {
    item() {
      this.updateObjectUrl()
    },
  },
  computed: {
    fileSize(): string | undefined {
      if (this.item instanceof File) {
        return getFileSize(this.item.size)
      }
      return getFileSize(this.$_.get(this.item, 'fileSize', undefined))
    },
    dimension(): Dimension | undefined {
      if (this.$_.has(this.item, 'width') && this.$_.has(this.item, 'height')) {
        return {width: this.$_.get(this.item, 'width', 0), height: this.$_.get(this.item, 'height', 0)}
      }
      return undefined
    },
    mediaType(): string | undefined {
      return this.$_.get(this.item, 'mediaType', undefined)
    },
    fileTooBig(): boolean {
      if (this.item instanceof File) {
        return this.item.size > 1_000_000
      } else {
        return false
      }
    },
    statusIcon(): string {
      if (this.item instanceof File) {
        if (this.fileTooBig) {
          return 'mdi-alert-circle'
        } else {
          return 'mdi-cloud-upload-outline'
        }
      } else {
        if (this.item.type === 'SIDECAR') {
          return 'mdi-folder-outline'
        } else if (this.item.type === 'GENERATED') {
          return 'mdi-file-outline'
        } else {
          return 'mdi-cloud-check-outline'
        }
      }
    },
    statusTooltip(): string {
      if (this.item instanceof File) {
        if (this.fileTooBig) {
          return this.$t('thumbnail_card.tooltip_too_big').toString()
        } else {
          return this.$t('thumbnail_card.tooltip_to_be_uploaded').toString()
        }
      } else {
        if (this.item.type === 'SIDECAR') {
          return this.$t('thumbnail_card.tooltip_sidecar').toString()
        }
        if (this.item.type === 'GENERATED') {
          return this.$t('thumbnail_card.tooltip_generated').toString()
        } else {
          return this.$t('thumbnail_card.tooltip_user_uploaded').toString()
        }
      }
    },
    imageUrl(): string {
      if (this.item instanceof File) {
        return this.objectUrl || ''
      } else if ('seriesId' in this.item) {
        return seriesThumbnailUrlByThumbnailId(this.item.seriesId, this.item.id)
      } else if ('bookId' in this.item) {
        return bookThumbnailUrlByThumbnailId(this.item.bookId, this.item.id)
      } else if ('readListId' in this.item) {
        return readListThumbnailUrlByThumbnailId(this.item.readListId, this.item.id)
      } else if ('collectionId' in this.item) {
        return collectionThumbnailUrlByThumbnailId(this.item.collectionId, this.item.id)
      } else {
        throw new Error('The given item type is not known!')
      }
    },
    deletable() {
      if (this.item instanceof File) {
        return true
      } else {
        return this.item.type !== 'SIDECAR' && this.item.type !== 'GENERATED'
      }
    },
  },
  methods: {
    revokeObjectUrl() {
      if (this.objectUrl) URL.revokeObjectURL(this.objectUrl)
      this.objectUrl = undefined
    },
    updateObjectUrl() {
      this.revokeObjectUrl()
      if (this.item instanceof File) this.objectUrl = URL.createObjectURL(this.item)
    },
    onClickSelect() {
      if (!this.selected) {
        this.$emit('on-select-thumbnail', this.item)
      }
    },
    onClickDelete() {
      this.$emit('on-delete-thumbnail', this.item)
    },
  },
})
</script>

<style scoped>
.thumbnail-card {
  overflow: hidden;
  border-color: var(--k-border);
  background: var(--k-surface-card);
}

.thumbnail-card__cover {
  aspect-ratio: var(--k-cover-aspect-ratio);
  background: var(--k-surface-muted);
}
</style>
