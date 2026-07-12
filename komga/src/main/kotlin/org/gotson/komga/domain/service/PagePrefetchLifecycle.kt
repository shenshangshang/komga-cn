package org.gotson.komga.domain.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.gotson.komga.domain.model.Book
import org.gotson.komga.domain.model.TypedBytes
import org.gotson.komga.domain.persistence.MediaRepository
import org.gotson.komga.infrastructure.cache.PagePrefetchCache
import org.gotson.komga.infrastructure.configuration.KomgaSettingsProvider
import org.gotson.komga.infrastructure.image.ImageType
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class PagePrefetchLifecycle(
  private val pagePrefetchCache: PagePrefetchCache,
  private val mediaRepository: MediaRepository,
  private val bookLifecycle: BookLifecycle,
  private val komgaSettingsProvider: KomgaSettingsProvider,
) {
  fun getPageWithPrefetch(
    book: Book,
    pageNumber: Int,
    convertFormat: ImageType?,
  ): TypedBytes {
    val bookId = book.id
    val cached = pagePrefetchCache.get(bookId, pageNumber)
    if (cached != null) {
      logger.debug { "Cache hit: book=$bookId, page=$pageNumber" }
      return TypedBytes(cached, "image/jpeg")
    }
    val pageContent = bookLifecycle.getBookPage(book, pageNumber, convertFormat)
    pagePrefetchCache.put(bookId, pageNumber, pageContent.bytes)
    triggerPrefetch(book, pageNumber, convertFormat)
    return pageContent
  }

  @Async
  fun triggerPrefetch(
    book: Book,
    currentPage: Int,
    convertFormat: ImageType?,
  ) {
    val prefetchCount = komgaSettingsProvider.prefetchPages
    if (prefetchCount <= 0) return
    val media = mediaRepository.findById(book.id)
    val totalPages = media.pages.size
    for (i in 1..prefetchCount) {
      val nextPage = currentPage + i
      if (nextPage > totalPages) break
      if (pagePrefetchCache.get(book.id, nextPage) != null) continue
      try {
        val pageContent = bookLifecycle.getBookPage(book, nextPage, convertFormat)
        pagePrefetchCache.prefetch(book.id, nextPage, pageContent.bytes)
      } catch (e: Exception) {
        logger.warn(e) { "Prefetch failed: book=${book.id}, page=$nextPage" }
        break
      }
    }
  }
}
