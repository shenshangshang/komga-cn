package org.gotson.komga.infrastructure.cache

import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.domain.model.TypedBytes
import org.gotson.komga.infrastructure.image.ImageType
import org.junit.jupiter.api.Test

class PagePrefetchCacheTest {
  @Test
  fun `cache is bounded by payload bytes`() {
    val cache = PagePrefetchCache(maximumWeightBytes = 5)

    cache.put("book", 1, ImageType.PNG, TypedBytes(ByteArray(4), "image/png"))
    cache.put("book", 2, ImageType.PNG, TypedBytes(ByteArray(4), "image/png"))
    cache.cleanUp()

    assertThat(cache.estimatedSize()).isLessThanOrEqualTo(1)
  }

  @Test
  fun `cache keeps image representations isolated`() {
    val cache = PagePrefetchCache()
    val png = TypedBytes(byteArrayOf(1), "image/png")
    val jpeg = TypedBytes(byteArrayOf(2), "image/jpeg")

    cache.put("book", 1, ImageType.PNG, png)
    cache.put("book", 1, ImageType.JPEG, jpeg)

    assertThat(cache.get("book", 1, ImageType.PNG)?.bytes).containsExactly(1)
    assertThat(cache.get("book", 1, ImageType.PNG)?.mediaType).isEqualTo("image/png")
    assertThat(cache.get("book", 1, ImageType.JPEG)?.bytes).containsExactly(2)
    assertThat(cache.get("book", 1, ImageType.JPEG)?.mediaType).isEqualTo("image/jpeg")
  }
}
