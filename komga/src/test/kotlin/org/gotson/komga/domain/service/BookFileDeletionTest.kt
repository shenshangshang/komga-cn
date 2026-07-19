package org.gotson.komga.domain.service

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files

class BookFileDeletionTest {
  @Test
  fun `deleteBookStorage deletes a regular book file`() {
    Jimfs.newFileSystem(Configuration.unix()).use { fs ->
      val book = Files.createFile(fs.getPath("/book.cbz"))

      book.deleteBookStorage()

      assertThat(Files.notExists(book)).isTrue()
    }
  }

  @Test
  fun `deleteBookStorage deletes an image directory recursively`() {
    Jimfs.newFileSystem(Configuration.unix()).use { fs ->
      val book = Files.createDirectories(fs.getPath("/book/chapter-1"))
      Files.createFile(book.resolve("001.jpg"))
      Files.createDirectories(fs.getPath("/book/chapter-2/nested"))
      Files.createFile(fs.getPath("/book/chapter-2/nested/002.png"))

      fs.getPath("/book").deleteBookStorage()

      assertThat(Files.notExists(fs.getPath("/book"))).isTrue()
    }
  }

  @Test
  fun `deleteBookStorage retries when a CIFS directory gains a late entry`() {
    Jimfs.newFileSystem(Configuration.unix()).use { fs ->
      val book = Files.createDirectories(fs.getPath("/book"))
      Files.createFile(book.resolve("001.jpg"))
      var injectedLateEntry = false

      BookStorageDeleter(
        maxAttempts = 3,
        retryDelayMillis = 0,
        beforeDirectoryDelete = { directory, attempt ->
          if (directory == book && attempt == 1 && !injectedLateEntry) {
            Files.createFile(book.resolve("late-from-cifs-cache.jpg"))
            injectedLateEntry = true
          }
        },
      ).delete(book)

      assertThat(injectedLateEntry).isTrue()
      assertThat(Files.notExists(book)).isTrue()
    }
  }
}
