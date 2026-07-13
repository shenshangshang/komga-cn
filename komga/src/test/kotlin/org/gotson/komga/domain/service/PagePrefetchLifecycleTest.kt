package org.gotson.komga.domain.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.domain.model.TypedBytes
import org.gotson.komga.domain.model.makeBook
import org.gotson.komga.infrastructure.cache.PagePrefetchCache
import org.gotson.komga.infrastructure.image.ImageType
import org.junit.jupiter.api.Test

class PagePrefetchLifecycleTest {
  private val cache = PagePrefetchCache()
  private val bookLifecycle = mockk<BookLifecycle>()
  private val worker = mockk<PagePrefetchWorker>(relaxed = true)
  private val lifecycle = PagePrefetchLifecycle(cache, bookLifecycle, worker)

  @Test
  fun `cache hit preserves the original media type and avoids rereading the page`() {
    val book = makeBook("book")
    every { bookLifecycle.getBookPage(book, 1, ImageType.PNG) } returns
      TypedBytes(byteArrayOf(1, 2, 3), "image/png")

    lifecycle.getPageWithPrefetch(book, 1, ImageType.PNG)
    val cached = lifecycle.getPageWithPrefetch(book, 1, ImageType.PNG)

    assertThat(cached.mediaType).isEqualTo("image/png")
    assertThat(cached.bytes).containsExactly(1, 2, 3)
    verify(exactly = 1) { bookLifecycle.getBookPage(book, 1, ImageType.PNG) }
  }

  @Test
  fun `requesting another conversion format does not reuse incompatible bytes`() {
    val book = makeBook("book")
    every { bookLifecycle.getBookPage(book, 1, ImageType.PNG) } returns
      TypedBytes(byteArrayOf(1), "image/png")
    every { bookLifecycle.getBookPage(book, 1, ImageType.JPEG) } returns
      TypedBytes(byteArrayOf(2), "image/jpeg")

    val png = lifecycle.getPageWithPrefetch(book, 1, ImageType.PNG)
    val jpeg = lifecycle.getPageWithPrefetch(book, 1, ImageType.JPEG)

    assertThat(png.bytes).containsExactly(1)
    assertThat(jpeg.bytes).containsExactly(2)
    verify(exactly = 1) { bookLifecycle.getBookPage(book, 1, ImageType.PNG) }
    verify(exactly = 1) { bookLifecycle.getBookPage(book, 1, ImageType.JPEG) }
  }

  @Test
  fun `cache miss schedules prefetch through the async worker`() {
    val book = makeBook("book")
    every { bookLifecycle.getBookPage(book, 1, null) } returns
      TypedBytes(byteArrayOf(1), "image/webp")

    lifecycle.getPageWithPrefetch(book, 1, null)

    verify(exactly = 1) { worker.triggerPrefetch(book, 1, null) }
  }
}
