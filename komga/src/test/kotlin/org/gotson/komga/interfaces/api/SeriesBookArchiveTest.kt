package org.gotson.komga.interfaces.api

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.domain.model.makeBook
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class SeriesBookArchiveTest {
  @Test
  fun `directory book starts the outer archive before generating its cbz`() {
    val root = Files.createTempDirectory("series-book-streaming")
    val directoryBook = Files.createDirectories(root.resolve("01"))
    val book = makeBook("01", url = directoryBook.toUri().toURL())
    val output = ByteArrayOutputStream()
    val directoryBookArchive = mockk<DirectoryBookArchive>()
    every { directoryBookArchive.write(directoryBook, any()) } answers {
      assertThat(output.size()).isGreaterThan(0)
      ZipOutputStream(secondArg()).use { innerZip ->
        innerZip.putNextEntry(ZipEntry("001.jpg"))
        innerZip.write(byteArrayOf(1, 2, 3))
        innerZip.closeEntry()
      }
    }

    SeriesBookArchive(directoryBookArchive).write(listOf(book), "", output)

    val innerArchive = readZip(readZip(output.toByteArray()).getValue("01.cbz"))
    assertThat(innerArchive["001.jpg"]).containsExactly(1, 2, 3)
  }

  @Test
  fun `series archive preserves directories and converts directory books to cbz`() {
    val root = Files.createTempDirectory("series-book-archive")
    val regularBook = Files.createDirectories(root.resolve("第一部")).resolve("01.cbz")
    Files.write(regularBook, byteArrayOf(1, 2, 3))
    val directoryBook = Files.createDirectories(root.resolve("第二部/02"))
    Files.write(directoryBook.resolve("001.jpg"), byteArrayOf(4, 5))
    Files.write(directoryBook.resolve("002.jpg"), byteArrayOf(6, 7))
    val books =
      listOf(
        makeBook("01", url = regularBook.toUri().toURL()).copy(directoryPath = "第一部", number = 1),
        makeBook("02", url = directoryBook.toUri().toURL()).copy(directoryPath = "第二部", number = 2),
      )
    val output = ByteArrayOutputStream()

    SeriesBookArchive(DirectoryBookArchive()).write(books, "", output)

    val outerEntries = readZip(output.toByteArray())
    assertThat(outerEntries.keys).containsExactly("第一部/01.cbz", "第二部/02.cbz")
    assertThat(outerEntries["第一部/01.cbz"]).containsExactly(1, 2, 3)
    val directoryBookEntries = readZip(outerEntries.getValue("第二部/02.cbz"))
    assertThat(directoryBookEntries.keys).containsExactly("001.jpg", "002.jpg")
    assertThat(directoryBookEntries["001.jpg"]).containsExactly(4, 5)
  }

  @Test
  fun `folder archive strips selected directory prefix and includes descendants`() {
    val root = Files.createTempDirectory("series-folder-archive")
    val directBook = Files.createDirectories(root.resolve("第一部")).resolve("01.cbz")
    val nestedBook = Files.createDirectories(root.resolve("第一部/第一集")).resolve("02.cbz")
    Files.write(directBook, byteArrayOf(1))
    Files.write(nestedBook, byteArrayOf(2))
    val books =
      listOf(
        makeBook("01", url = directBook.toUri().toURL()).copy(directoryPath = "第一部", number = 1),
        makeBook("02", url = nestedBook.toUri().toURL()).copy(directoryPath = "第一部/第一集", number = 2),
      )
    val output = ByteArrayOutputStream()

    SeriesBookArchive(DirectoryBookArchive()).write(books, "第一部", output)

    assertThat(readZip(output.toByteArray()).keys).containsExactly("01.cbz", "第一集/02.cbz")
  }

  private fun readZip(bytes: ByteArray): LinkedHashMap<String, ByteArray> {
    val entries = linkedMapOf<String, ByteArray>()
    ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
      var entry = zip.nextEntry
      while (entry != null) {
        entries[entry.name] = zip.readBytes()
        entry = zip.nextEntry
      }
    }
    return entries
  }
}
