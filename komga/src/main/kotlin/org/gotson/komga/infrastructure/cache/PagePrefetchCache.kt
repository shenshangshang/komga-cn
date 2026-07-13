package org.gotson.komga.infrastructure.cache

import com.github.benmanes.caffeine.cache.Caffeine
import org.gotson.komga.domain.model.TypedBytes
import org.gotson.komga.infrastructure.image.ImageType
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class PagePrefetchCache(
  maximumWeightBytes: Long = DEFAULT_MAXIMUM_WEIGHT_BYTES,
) {
  private val cache =
    Caffeine
      .newBuilder()
      .maximumWeight(maximumWeightBytes)
      .weigher<String, TypedBytes> { _, value -> value.bytes.size }
      .expireAfterWrite(10, TimeUnit.MINUTES)
      .recordStats()
      .build<String, TypedBytes>()

  fun get(
    bookId: String,
    pageNumber: Int,
    convertFormat: ImageType?,
  ): TypedBytes? = cache.getIfPresent(key(bookId, pageNumber, convertFormat))

  fun put(
    bookId: String,
    pageNumber: Int,
    convertFormat: ImageType?,
    data: TypedBytes,
  ) {
    cache.put(key(bookId, pageNumber, convertFormat), data)
  }

  fun prefetch(
    bookId: String,
    pageNumber: Int,
    convertFormat: ImageType?,
    data: TypedBytes,
  ) {
    put(bookId, pageNumber, convertFormat, data)
  }

  internal fun cleanUp() = cache.cleanUp()

  internal fun estimatedSize(): Long = cache.estimatedSize()

  private fun key(
    bookId: String,
    page: Int,
    convertFormat: ImageType?,
  ) = "$bookId:$page:${convertFormat?.name ?: "ORIGINAL"}"

  private companion object {
    const val DEFAULT_MAXIMUM_WEIGHT_BYTES = 256L * 1024 * 1024
  }
}
