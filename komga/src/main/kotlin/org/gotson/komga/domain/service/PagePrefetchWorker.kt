package org.gotson.komga.domain.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.gotson.komga.domain.model.Book
import org.gotson.komga.domain.persistence.MediaRepository
import org.gotson.komga.infrastructure.cache.PagePrefetchCache
import org.gotson.komga.infrastructure.configuration.KomgaSettingsProvider
import org.gotson.komga.infrastructure.image.ImageType
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class PagePrefetchWorker(
  private val pagePrefetchCache: PagePrefetchCache,
  private val mediaRepository: MediaRepository,
  private val bookLifecycle: BookLifecycle,
  private val komgaSettingsProvider: KomgaSettingsProvider,
) {
  @Async
  fun triggerPrefetch(
    book: Book,
    currentPage: Int,
    convertFormat: ImageType?,
  ) {
    val prefetchCount = komgaSettingsProvider.prefetchPages
    if (prefetchCount <= 0) return

    val totalPages = mediaRepository.findById(book.id).pages.size
    for (nextPage in (currentPage + 1)..minOf(currentPage + prefetchCount, totalPages)) {
      if (pagePrefetchCache.get(book.id, nextPage, convertFormat) != null) continue
      try {
        val pageContent = bookLifecycle.getBookPage(book, nextPage, convertFormat)
        pagePrefetchCache.prefetch(book.id, nextPage, convertFormat, pageContent)
      } catch (e: Exception) {
        logger.warn(e) { "Prefetch failed: book=${book.id}, page=$nextPage" }
        break
      }
    }
  }
}
