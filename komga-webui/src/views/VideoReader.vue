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
        <v-btn
          icon
          class="k-touch-target"
          :disabled="!screenfull.isEnabled"
          :aria-label="$t('bookreader.shortcuts.fullscreen')"
          @click="screenfull.isFullscreen ? screenfull.exit() : enterFullscreen()"
        >
          <v-icon>{{ fullscreenIcon }}</v-icon>
        </v-btn>
      </v-toolbar>
    </v-slide-y-transition>

    <div class="video-container" @click="toggleToolbars">
      <video
        ref="player"
        controls
        playsinline
        :preload="'auto'"
        class="video-element"
      ></video>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import screenfull from 'screenfull'
import {debounce} from 'lodash'
import {getBookReadRouteFromMedia} from '@/functions/book-format'
import {bookStreamUrl} from '@/functions/urls'
import {getBookTitleCompact} from '@/functions/book-title'
import {BookDto, ReadProgressUpdateDto} from '@/types/komga-books'

const Plyr = require('plyr')

export default Vue.extend({
  name: 'VideoReader',
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
      screenfull,
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
    fullscreenIcon(): string {
      return screenfull.isFullscreen ? 'mdi-fullscreen-exit' : 'mdi-fullscreen'
    },
  },
  async mounted() {
    if (screenfull.isEnabled) screenfull.on('change', this.fullscreenChanged)
    await this.setup(this.bookId)
  },
  beforeDestroy() {
    this.destroyPlayer()
    if (screenfull.isEnabled) {
      screenfull.off('change', this.fullscreenChanged)
      screenfull.exit()
    }
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
      const video = this.$refs.player as HTMLVideoElement
      if (!video) return

      video.src = bookStreamUrl(this.bookId)

      this.player = new Plyr(video, {
        controls: ['play-large', 'play', 'progress', 'current-time', 'duration', 'mute', 'volume', 'settings', 'pip', 'airplay', 'fullscreen'],
        settings: ['speed', 'quality'],
        autoplay: false,
        ratio: null,
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
    fullscreenChanged() {
      this.$forceUpdate()
    },
    enterFullscreen() {
      if (screenfull.isEnabled) screenfull.request(document.documentElement, {navigationUI: 'hide'})
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

.video-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #000;
}
.video-element {
  max-width: 100%;
  max-height: 100vh;
}
</style>
