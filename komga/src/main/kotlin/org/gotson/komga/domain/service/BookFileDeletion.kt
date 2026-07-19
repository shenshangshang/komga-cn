package org.gotson.komga.domain.service

import java.nio.file.DirectoryNotEmptyException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

internal class BookStorageDeleter(
  private val maxAttempts: Int = 5,
  private val retryDelayMillis: Long = 100,
  private val beforeDirectoryDelete: (Path, Int) -> Unit = { _, _ -> },
) {
  fun delete(path: Path) {
    require(maxAttempts > 0) { "maxAttempts must be greater than zero" }
    if (Files.notExists(path)) return

    if (!Files.isDirectory(path) || Files.isSymbolicLink(path)) {
      Files.delete(path)
      return
    }

    var lastDirectoryNotEmpty: DirectoryNotEmptyException? = null
    for (attempt in 1..maxAttempts) {
      try {
        deleteDirectoryPass(path, attempt)
        if (Files.notExists(path)) return
      } catch (e: DirectoryNotEmptyException) {
        lastDirectoryNotEmpty = e
      } catch (_: NoSuchFileException) {
        if (Files.notExists(path)) return
      }

      if (attempt < maxAttempts && retryDelayMillis > 0) Thread.sleep(retryDelayMillis * attempt)
    }

    throw lastDirectoryNotEmpty ?: DirectoryNotEmptyException(path.toString())
  }

  private fun deleteDirectoryPass(
    path: Path,
    attempt: Int,
  ) {
    Files.walkFileTree(
      path,
      object : SimpleFileVisitor<Path>() {
        override fun visitFile(
          file: Path,
          attrs: BasicFileAttributes,
        ): FileVisitResult {
          Files.deleteIfExists(file)
          return FileVisitResult.CONTINUE
        }

        override fun postVisitDirectory(
          dir: Path,
          exc: java.io.IOException?,
        ): FileVisitResult {
          if (exc != null) throw exc
          beforeDirectoryDelete(dir, attempt)
          Files.deleteIfExists(dir)
          return FileVisitResult.CONTINUE
        }
      },
    )
  }
}

internal fun Path.deleteBookStorage() = BookStorageDeleter().delete(this)
