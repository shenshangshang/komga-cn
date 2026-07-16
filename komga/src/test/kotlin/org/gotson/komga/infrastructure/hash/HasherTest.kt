package org.gotson.komga.infrastructure.hash

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class HasherTest {
  private val hasher = Hasher()

  @Test
  fun `given directory when hashing then hash is stable and changes with page content`(
    @TempDir directory: Path,
  ) {
    Files.writeString(directory.resolve("001.jpg"), "page one")
    Files.writeString(directory.resolve("002.jpg"), "page two")

    val first = hasher.computeHash(directory)
    val second = hasher.computeHash(directory)
    Files.writeString(directory.resolve("002.jpg"), "updated page two")
    val updated = hasher.computeHash(directory)

    assertThat(first).isEqualTo(second)
    assertThat(updated).isNotEqualTo(first)
  }
}
