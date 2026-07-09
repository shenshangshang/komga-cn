package org.gotson.komga.infrastructure.cache

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class PagePrefetchCache {
  private val cache =
    Caffeine
      .newBuilder()
      .maximumSize(500)
      .expireAfterWrite(10, TimeUnit.MINUTES)
      .recordStats()
      .build<String, ByteArray>()

  fun get(bookId: String, pageNumber: Int): ByteArray? = 
    cache.getIfPresent(key(bookId, pageNumber))

  fun put(bookId: String, pageNumber: Int, data: ByteArray) {
    cache.put(key(bookId, pageNumber), data)
  }

  fun prefetch(bookId: String, pageNumber: Int, data: ByteArray) {
    cache.put(key(bookId, pageNumber), data)
  }

  private fun key(bookId: String, page: Int) = "$bookId:$page"
}
