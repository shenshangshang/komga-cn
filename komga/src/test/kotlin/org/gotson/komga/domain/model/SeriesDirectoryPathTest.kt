package org.gotson.komga.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class SeriesDirectoryPathTest {
  @Test
  fun `valid relative directory paths are retained`() {
    assertThat(normalizeSeriesDirectoryPath("")).isEmpty()
    assertThat(normalizeSeriesDirectoryPath("第一部/第一集")).isEqualTo("第一部/第一集")
  }

  @Test
  fun `absolute traversal and empty segments are rejected`() {
    listOf("/absolute", "C:/absolute", "trailing/", "../escape", "part/../escape", "part//episode", "part\\episode", "%2e%2e/escape").forEach { path ->
      assertThatIllegalArgumentException().isThrownBy { normalizeSeriesDirectoryPath(path) }
    }
  }
}
