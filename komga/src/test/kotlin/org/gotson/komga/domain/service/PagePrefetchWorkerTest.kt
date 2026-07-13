package org.gotson.komga.domain.service

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.gotson.komga.domain.model.Media
import org.gotson.komga.domain.model.TypedBytes
import org.gotson.komga.domain.model.makeBook
import org.gotson.komga.domain.model.makeBookPage
import org.gotson.komga.domain.persistence.MediaRepository
import org.gotson.komga.infrastructure.cache.PagePrefetchCache
import org.gotson.komga.infrastructure.configuration.KomgaSettingsProvider
import org.gotson.komga.infrastructure.image.ImageType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PagePrefetchWorkerTest {
  private val cache = mockk<PagePrefetchCache>(relaxed = true)
  private val mediaRepository = mockk<MediaRepository>()
  private val bookLifecycle = mockk<BookLifecycle>()
  private val settingsProvider = mockk<KomgaSettingsProvider>()
  private val worker = PagePrefetchWorker(cache, mediaRepository, bookLifecycle, settingsProvider)

  @BeforeEach
  fun clearRecordedCalls() {
    clearMocks(cache, mediaRepository, bookLifecycle, settingsProvider, answers = false)
  }

  @Test
  fun `prefetch is bounded by setting and media page count`() {
    val book = makeBook("book")
    every { settingsProvider.prefetchPages } returns 4
    every { mediaRepository.findById(book.id) } returns
      Media(bookId = book.id, pages = mutableListOf(makeBookPage("1"), makeBookPage("2"), makeBookPage("3")))
    every { cache.get(book.id, any(), ImageType.PNG) } returns null
    every { bookLifecycle.getBookPage(book, any(), ImageType.PNG) } answers
      { TypedBytes(byteArrayOf(secondArg<Int>().toByte()), "image/png") }

    worker.triggerPrefetch(book, 1, ImageType.PNG)

    verify(exactly = 1) { bookLifecycle.getBookPage(book, 2, ImageType.PNG) }
    verify(exactly = 1) { bookLifecycle.getBookPage(book, 3, ImageType.PNG) }
    verify(exactly = 0) { bookLifecycle.getBookPage(book, 4, ImageType.PNG) }
  }

  @Test
  fun `prefetch skips an already cached representation`() {
    val book = makeBook("book")
    every { settingsProvider.prefetchPages } returns 2
    every { mediaRepository.findById(book.id) } returns
      Media(bookId = book.id, pages = mutableListOf(makeBookPage("1"), makeBookPage("2"), makeBookPage("3")))
    every { cache.get(book.id, 2, null) } returns TypedBytes(byteArrayOf(2), "image/webp")
    every { cache.get(book.id, 3, null) } returns null
    every { bookLifecycle.getBookPage(book, 3, null) } returns TypedBytes(byteArrayOf(3), "image/webp")

    worker.triggerPrefetch(book, 1, null)

    verify(exactly = 0) { bookLifecycle.getBookPage(book, 2, null) }
    verify(exactly = 1) { bookLifecycle.getBookPage(book, 3, null) }
  }

  @Test
  fun `disabled prefetch does not query media or pages`() {
    val book = makeBook("book")
    every { settingsProvider.prefetchPages } returns 0

    worker.triggerPrefetch(book, 1, null)

    verify(exactly = 0) { mediaRepository.findById(any()) }
    verify(exactly = 0) { bookLifecycle.getBookPage(any(), any(), any()) }
  }
}
