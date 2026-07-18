package org.gotson.komga.interfaces.api

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.compress.archivers.zip.Zip64Mode
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.io.IOUtils
import org.gotson.komga.domain.model.Book
import org.springframework.stereotype.Component
import java.io.FilterOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.Deflater
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

@Component
class SeriesBookArchive(
  private val directoryBookArchive: DirectoryBookArchive,
) {
  fun write(
    books: Collection<Book>,
    baseDirectoryPath: String,
    output: OutputStream,
  ) {
    ZipArchiveOutputStream(output).use { zipStream ->
      zipStream.setMethod(ZipArchiveOutputStream.DEFLATED)
      zipStream.setLevel(Deflater.NO_COMPRESSION)
      zipStream.setUseZip64(Zip64Mode.AsNeeded)

      books
        .sortedWith(compareBy<Book> { it.directoryPath }.thenBy { it.number }.thenBy { it.name })
        .forEach { book ->
          when {
            Files.isDirectory(book.path) -> addDirectoryBook(zipStream, book, baseDirectoryPath)
            Files.isRegularFile(book.path) -> addRegularBook(zipStream, book, baseDirectoryPath)
            else -> logger.warn { "Book file not found, skipping archive entry: ${book.path}" }
          }
        }
    }
  }

  private fun addDirectoryBook(
    zipStream: ZipArchiveOutputStream,
    book: Book,
    baseDirectoryPath: String,
  ) {
    val entryName = archiveEntryName(book, baseDirectoryPath, "${book.path.name}.cbz")
    logger.debug { "Adding directory book to series archive: ${book.path} as $entryName" }
    zipStream.putArchiveEntry(ZipArchiveEntry(entryName))
    zipStream.flush()
    directoryBookArchive.write(book.path, NonClosingOutputStream(zipStream))
    zipStream.closeArchiveEntry()
  }

  private fun addRegularBook(
    zipStream: ZipArchiveOutputStream,
    book: Book,
    baseDirectoryPath: String,
  ) {
    val entryName = archiveEntryName(book, baseDirectoryPath, book.path.name)
    logger.debug { "Adding file to series archive: ${book.path} as $entryName" }
    addFile(zipStream, book.path, entryName)
  }

  private fun addFile(
    zipStream: ZipArchiveOutputStream,
    path: Path,
    entryName: String,
  ) {
    Files.newInputStream(path).use {
      zipStream.putArchiveEntry(ZipArchiveEntry(entryName).apply { size = Files.size(path) })
      zipStream.flush()
      IOUtils.copyLarge(it, zipStream, ByteArray(DEFAULT_BUFFER_SIZE))
      zipStream.closeArchiveEntry()
    }
  }

  private fun archiveEntryName(
    book: Book,
    baseDirectoryPath: String,
    fileName: String,
  ): String {
    val relativeDirectory =
      if (baseDirectoryPath.isEmpty()) {
        book.directoryPath
      } else {
        book.directoryPath.removePrefix(baseDirectoryPath).removePrefix("/")
      }
    return listOf(relativeDirectory, fileName).filter { it.isNotEmpty() }.joinToString("/")
  }
}

private class NonClosingOutputStream(
  output: OutputStream,
) : FilterOutputStream(output) {
  override fun write(
    bytes: ByteArray,
    offset: Int,
    length: Int,
  ) = out.write(bytes, offset, length)

  override fun close() = flush()
}
