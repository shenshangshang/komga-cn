package org.gotson.komga.domain.service

import java.nio.file.Path
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

internal fun Path.deleteBookStorage() {
  if (Files.notExists(this)) return

  if (!Files.isDirectory(this) || Files.isSymbolicLink(this)) {
    Files.delete(this)
    return
  }

  Files.walkFileTree(
    this,
    object : SimpleFileVisitor<Path>() {
      override fun visitFile(
        file: Path,
        attrs: BasicFileAttributes,
      ): FileVisitResult {
        Files.delete(file)
        return FileVisitResult.CONTINUE
      }

      override fun postVisitDirectory(
        dir: Path,
        exc: java.io.IOException?,
      ): FileVisitResult {
        if (exc != null) throw exc
        Files.delete(dir)
        return FileVisitResult.CONTINUE
      }
    },
  )
}
