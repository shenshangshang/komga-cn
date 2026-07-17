package org.gotson.komga.interfaces.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipInputStream

class DirectoryBookArchiveTest {
  @Test
  fun `directory book is streamed as a cbz with visible regular files`() {
    val directory = Files.createTempDirectory("directory-book-download")
    Files.write(directory.resolve("001.jpg"), byteArrayOf(1, 2, 3))
    Files.write(directory.resolve("002.png"), byteArrayOf(4, 5))
    Files.write(directory.resolve(".hidden.jpg"), byteArrayOf(9))
    Files.createDirectory(directory.resolve("nested"))
    Files.write(directory.resolve("nested/ignored.jpg"), byteArrayOf(8))
    val output = ByteArrayOutputStream()

    DirectoryBookArchive().write(directory, output)

    val entries = mutableMapOf<String, ByteArray>()
    ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
      var entry = zip.nextEntry
      while (entry != null) {
        entries[entry.name] = zip.readBytes()
        entry = zip.nextEntry
      }
    }
    assertThat(entries.keys).containsExactly("001.jpg", "002.png")
    assertThat(entries["001.jpg"]).containsExactly(1, 2, 3)
    assertThat(entries["002.png"]).containsExactly(4, 5)
  }
}
