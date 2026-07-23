package org.gotson.komga.domain.service

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.file.Files

class BookFileDeletionTest {
  @Test
  fun `shared directory deletion removes only analyzed pages and preserves nested sibling storage`() {
    Jimfs.newFileSystem(Configuration.unix()).use { fs ->
      val series = fs.getPath("/series")
      Files.createDirectories(series.resolve("sibling"))
      Files.createFile(series.resolve("loose-page.jpg"))
      Files.createFile(series.resolve("sibling/page.jpg"))

      series.deleteBookStorage(
        pageFileNames = listOf("loose-page.jpg"),
        protectedDescendantPaths = listOf(series.resolve("sibling")),
      )

      assertThat(Files.notExists(series.resolve("loose-page.jpg"))).isTrue()
      assertThat(Files.exists(series.resolve("sibling/page.jpg"))).isTrue()
      assertThat(Files.exists(series)).isTrue()
    }
  }

  @Test
  fun `shared directory deletion refuses path traversal`() {
    Jimfs.newFileSystem(Configuration.unix()).use { fs ->
      val series = fs.getPath("/series")
      Files.createDirectories(series.resolve("sibling"))
      Files.createFile(fs.getPath("/outside.jpg"))

      assertThatThrownBy {
        series.deleteBookStorage(
          pageFileNames = listOf("../outside.jpg"),
          protectedDescendantPaths = listOf(series.resolve("sibling")),
        )
      }.isInstanceOf(IllegalStateException::class.java)

      assertThat(Files.exists(fs.getPath("/outside.jpg"))).isTrue()
    }
  }

  @Test
  fun `shared directory deletion validates every page before deleting and refuses sibling pages`() {
    Jimfs.newFileSystem(Configuration.unix()).use { fs ->
      val series = fs.getPath("/series")
      val sibling = series.resolve("sibling")
      Files.createDirectories(sibling)
      Files.createFile(series.resolve("loose-page.jpg"))
      Files.createFile(sibling.resolve("page.jpg"))

      assertThatThrownBy {
        series.deleteBookStorage(
          pageFileNames = listOf("loose-page.jpg", "sibling/page.jpg"),
          protectedDescendantPaths = listOf(sibling),
        )
      }.isInstanceOf(IllegalStateException::class.java)

      assertThat(Files.exists(series.resolve("loose-page.jpg"))).isTrue()
      assertThat(Files.exists(sibling.resolve("page.jpg"))).isTrue()
    }
  }

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
