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

internal fun Path.deleteBookStorage(
  pageFileNames: Collection<String>,
  protectedDescendantPaths: Collection<Path>,
) {
  val root = normalize()
  val protectedPaths =
    protectedDescendantPaths
      .asSequence()
      .map { it.normalize() }
      .filter { it != root && it.startsWith(root) }
      .toList()

  if (protectedPaths.isEmpty()) {
    deleteBookStorage()
    return
  }

  check(pageFileNames.isNotEmpty()) {
    "Refusing to delete a shared book directory without an analyzed page list: $root"
  }

  val pagePaths =
    pageFileNames.map { fileName ->
    val pagePath = root.resolve(fileName).normalize()
    check(pagePath.startsWith(root)) {
      "Refusing to delete a page outside the book directory: $pagePath"
    }
    check(!Files.isDirectory(pagePath)) {
      "Refusing to recursively delete a nested directory from a shared book path: $pagePath"
    }
      check(protectedPaths.none { pagePath.startsWith(it) }) {
        "Refusing to delete a page belonging to sibling book storage: $pagePath"
      }
      pagePath
    }

  pagePaths.forEach(Files::deleteIfExists)
}
