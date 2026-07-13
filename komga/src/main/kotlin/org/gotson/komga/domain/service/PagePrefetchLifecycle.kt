package org.gotson.komga.domain.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.gotson.komga.domain.model.Book
import org.gotson.komga.domain.model.TypedBytes
import org.gotson.komga.infrastructure.cache.PagePrefetchCache
import org.gotson.komga.infrastructure.image.ImageType
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class PagePrefetchLifecycle(
  private val pagePrefetchCache: PagePrefetchCache,
  private val bookLifecycle: BookLifecycle,
  private val pagePrefetchWorker: PagePrefetchWorker,
) {
  fun getPageWithPrefetch(
    book: Book,
    pageNumber: Int,
    convertFormat: ImageType?,
  ): TypedBytes {
    val bookId = book.id
    val cached = pagePrefetchCache.get(bookId, pageNumber, convertFormat)
    if (cached != null) {
      logger.debug { "Cache hit: book=$bookId, page=$pageNumber" }
      return cached
    }
    val pageContent = bookLifecycle.getBookPage(book, pageNumber, convertFormat)
    pagePrefetchCache.put(bookId, pageNumber, convertFormat, pageContent)
    pagePrefetchWorker.triggerPrefetch(book, pageNumber, convertFormat)
    return pageContent
  }
}
