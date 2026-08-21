<template>
  <div id="root" class="reader-shell" :key="bookId">
    <v-slide-y-transition>
      <v-toolbar
        v-if="showToolbars"
        dense elevation="1"
        class="full-width"
        style="position: fixed; top: 0;z-index: 14"
      >
        <v-btn icon class="k-touch-target" :aria-label="$t('bookreader.shortcuts.close')" @click="closeBook">
          <v-icon>mdi-arrow-left</v-icon>
        </v-btn>
        <v-toolbar-title>{{ bookTitle }}</v-toolbar-title>
        <v-spacer></v-spacer>
      </v-toolbar>
    </v-slide-y-transition>

    <div class="audio-container" @click="toggleToolbars">
      <div class="audio-cover">
        <v-img
          v-if="coverUrl"
          :src="coverUrl"
          max-height="300"
          max-width="300"
          contain
        />
        <v-icon v-else size="120" color="grey">mdi-music-note</v-icon>
      </div>
      <audio ref="player" controls playsinline :preload="'auto'" class="audio-element"></audio>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import {debounce} from 'lodash'
import {getBookReadRouteFromMedia} from '@/functions/book-format'
import {bookStreamUrl, bookThumbnailUrl} from '@/functions/urls'
import {getBookTitleCompact} from '@/functions/book-title'
import {BookDto, ReadProgressUpdateDto} from '@/types/komga-books'

const Plyr = require('plyr')

export default Vue.extend({
  name: 'AudioReader',
  data() {
    return {
      book: {} as BookDto,
      series: {} as any,
      siblingPrevious: {} as BookDto,
      siblingNext: {} as BookDto,
      context: {} as any,
      incognito: false,
      showToolbars: true,
      player: null as any,
      seekedToProgress: false,
    }
  },
  props: {
    bookId: {
      type: String,
      required: true,
    },
  },
  computed: {
    bookTitle(): string {
      return getBookTitleCompact(this.book.metadata?.title, this.series.metadata?.title, this.book.oneshot ? undefined : this.book.metadata?.number)
    },
    coverUrl(): string {
      return bookThumbnailUrl(this.bookId)
    },
  },
  async mounted() {
    await this.setup(this.bookId)
  },
  beforeDestroy() {
    this.destroyPlayer()
  },
  beforeRouteUpdate(to, from, next) {
    if (to.params.bookId !== from.params.bookId) {
      this.destroyPlayer()
      this.setup(to.params.bookId)
    }
    next()
  },
  methods: {
    async setup(bookId: string) {
      this.book = await this.$komgaBooks.getBook(bookId)
      this.series = await this.$komgaSeries.getOneSeries(this.book.seriesId)

      this.incognito = !!(this.$route.query.incognito && this.$route.query.incognito.toString().toLowerCase() === 'true')

      this.context = {
        origin: this.$route.query.context as string || null,
        id: this.$route.query.contextId as string || null,
      }

      try {
        if (this.context.origin === 'COLLECTION') {
          this.siblingNext = await this.$komgaCollections.getBookSiblingNext(this.context.id, bookId)
          this.siblingPrevious = await this.$komgaCollections.getBookSiblingPrevious(this.context.id, bookId)
        } else if (this.context.origin === 'READLIST') {
          this.siblingNext = await this.$komgaReadLists.getBookSiblingNext(this.context.id, bookId)
          this.siblingPrevious = await this.$komgaReadLists.getBookSiblingPrevious(this.context.id, bookId)
        } else {
          this.siblingNext = await this.$komgaBooks.getBookSiblingNext(bookId)
          this.siblingPrevious = await this.$komgaBooks.getBookSiblingPrevious(bookId)
        }
      } catch (e) {
        // siblings may not be available
      }

      this.$nextTick(() => {
        this.initPlayer()
      })
    },
    initPlayer() {
      const audio = this.$refs.player as HTMLAudioElement
      if (!audio) return

      audio.src = bookStreamUrl(this.bookId)

      this.player = new Plyr(audio, {
        controls: ['play', 'progress', 'current-time', 'duration', 'mute', 'volume', 'settings'],
        settings: ['speed'],
        autoplay: false,
      })

      this.player.on('loadedmetadata', () => {
        if (!this.incognito && this.book.readProgress && this.book.readProgress.page > 0 && !this.seekedToProgress) {
          const targetTime = this.book.readProgress.page
          if (targetTime < this.player.duration) {
            this.player.currentTime = targetTime
          }
          this.seekedToProgress = true
        }
      })

      this.player.on('timeupdate', () => {
        if (!this.incognito && this.seekedToProgress) {
          this.markProgress(Math.floor(this.player.currentTime))
        }
      })

      this.player.on('ended', () => {
        if (!this.incognito) {
          this.markCompleted()
        }
      })
    },
    destroyPlayer() {
      if (this.player) {
        try { this.player.destroy() } catch (e) { /* ignore */ }
        this.player = null
      }
      this.seekedToProgress = false
    },
    markProgress: debounce(function (this: any, time: number) {
      if (!this.incognito) {
        const progress: ReadProgressUpdateDto = {page: time}
        this.$komgaBooks.updateReadProgress(this.bookId, progress)
      }
    }, 2000),
    markCompleted() {
      const progress: ReadProgressUpdateDto = {completed: true}
      this.$komgaBooks.updateReadProgress(this.bookId, progress)
    },
    toggleToolbars() {
      this.showToolbars = !this.showToolbars
    },
    closeBook() {
      this.$router.back()
    },
    previousBook() {
      if (!this.$_.isEmpty(this.siblingPrevious)) {
        this.$router.push({
          name: getBookReadRouteFromMedia(this.siblingPrevious.media),
          params: {bookId: this.siblingPrevious.id.toString()},
          query: {context: this.context.origin, contextId: this.context.id, folder: this.siblingPrevious.directoryPath || undefined, incognito: this.incognito.toString()},
        })
      }
    },
    nextBook() {
      if (this.$_.isEmpty(this.siblingNext)) {
        this.closeBook()
      } else {
        this.$router.push({
          name: getBookReadRouteFromMedia(this.siblingNext.media),
          params: {bookId: this.siblingNext.id.toString()},
          query: {context: this.context.origin, contextId: this.context.id, folder: this.siblingNext.directoryPath || undefined, incognito: this.incognito.toString()},
        })
      }
    },
  },
})
</script>

<style scoped>
.full-width {
  width: 100%;
}

.audio-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 80px 20px 20px;
}
.audio-cover {
  margin-bottom: 30px;
}
.audio-element {
  width: 100%;
  max-width: 600px;
}
</style>
