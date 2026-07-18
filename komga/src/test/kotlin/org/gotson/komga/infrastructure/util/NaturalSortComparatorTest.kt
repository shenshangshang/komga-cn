package org.gotson.komga.infrastructure.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class NaturalSortComparatorTest {
  @Test
  fun `natural sorting is safe across concurrent book analysis threads`() {
    val names = (1..500).map { "第${it}页-${it % 17}.jpg" }.reversed()
    val expected = names.sortedWith(NaturalSortComparator)
    val executor = Executors.newFixedThreadPool(8)

    try {
      val tasks =
        List(16) {
          Callable {
            repeat(50) {
              assertThat(names.sortedWith(NaturalSortComparator)).containsExactlyElementsOf(expected)
            }
            true
          }
        }

      assertThat(executor.invokeAll(tasks).map { it.get() }).containsOnly(true)
    } finally {
      executor.shutdownNow()
    }
  }
}
