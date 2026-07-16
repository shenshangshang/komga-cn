package org.gotson.komga.domain.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.gotson.komga.domain.model.Book
import org.gotson.komga.domain.model.DirectoryNotFoundException
import org.gotson.komga.domain.model.Library
import org.gotson.komga.domain.model.ScanResult
import org.gotson.komga.domain.model.Series
import org.gotson.komga.domain.model.Sidecar
import org.gotson.komga.infrastructure.sidecar.SidecarBookConsumer
import org.gotson.komga.infrastructure.sidecar.SidecarSeriesConsumer
import org.springframework.stereotype.Service
import java.io.IOException
import java.net.URL
import java.nio.file.FileVisitOption
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString
import kotlin.io.path.readAttributes
import kotlin.time.measureTime

private val logger = KotlinLogging.logger {}

private val imageFileExtensions =
  setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "tif", "tiff", "avif", "jxl")

@Service
class FileSystemScanner(
  private val sidecarBookConsumers: List<SidecarBookConsumer>,
  private val sidecarSeriesConsumers: List<SidecarSeriesConsumer>,
) {
  private data class TempSidecar(
    val name: String,
    val url: URL,
    val lastModifiedTime: LocalDateTime,
    val type: Sidecar.Type? = null,
  )

  private val sidecarBookPrefilter = sidecarBookConsumers.flatMap { it.getSidecarBookPrefilter() }

  fun scanRootFolder(
    root: Path,
    forceDirectoryModifiedTime: Boolean = false,
    oneshotsDir: String? = null,
    scanCbx: Boolean = true,
    scanPdf: Boolean = true,
    scanEpub: Boolean = true,
    scanMobi: Boolean = true,
    directoryExclusions: Set<String> = emptySet(),
    seriesGroupingMode: Library.SeriesGroupingMode = Library.SeriesGroupingMode.DIRECT_PARENT,
  ): ScanResult {
    val scanForExtensions =
      buildList {
        if (scanCbx) addAll(listOf("cbz", "zip", "cbr", "rar"))
        if (scanPdf) add("pdf")
        if (scanEpub) add("epub")
        if (scanMobi) add("mobi")
      }
    logger.info { "Scanning folder: $root" }
    logger.info { "Scan for extensions: $scanForExtensions" }
    logger.info { "Excluded directory patterns: $directoryExclusions" }
    logger.info { "Force directory modified time: $forceDirectoryModifiedTime" }

    if (!(Files.isDirectory(root) && Files.isReadable(root)))
      throw DirectoryNotFoundException("Folder is not accessible: $root", "ERR_1016")

    if (seriesGroupingMode == Library.SeriesGroupingMode.TOP_LEVEL) {
      return scanTopLevelSeries(
        root = root,
        scanForExtensions = scanForExtensions,
        forceDirectoryModifiedTime = forceDirectoryModifiedTime,
        oneshotsDir = oneshotsDir,
        directoryExclusions = directoryExclusions,
        scanImageDirectories = scanCbx,
      )
    }

    val scannedSeries = mutableMapOf<Series, List<Book>>()
    val scannedSidecars = mutableListOf<Sidecar>()

    measureTime {
      // path is the series directory
      val pathToSeries = mutableMapOf<Path, Series>()
      val pathToSeriesSidecars = mutableMapOf<Path, MutableList<Sidecar>>()
      // path is the book's parent directory, ie the series directory
      val pathToBooks = mutableMapOf<Path, MutableList<Book>>()
      val pathToBookSidecars = mutableMapOf<Path, MutableList<TempSidecar>>()
      val imageFilesByDirectory = mutableMapOf<Path, MutableList<Path>>()

      Files.walkFileTree(
        root,
        setOf(FileVisitOption.FOLLOW_LINKS),
        Integer.MAX_VALUE,
        object : FileVisitor<Path> {
          override fun preVisitDirectory(
            dir: Path,
            attrs: BasicFileAttributes,
          ): FileVisitResult {
            logger.trace { "preVisit: $dir (regularFile:${attrs.isRegularFile}, directory:${attrs.isDirectory}, symbolicLink:${attrs.isSymbolicLink}, other:${attrs.isOther})" }
            if (dir.name.startsWith(".") ||
              directoryExclusions.any { exclude ->
                dir.pathString.contains(exclude, true)
              }
            )
              return FileVisitResult.SKIP_SUBTREE

            pathToSeries[dir] =
              Series(
                name = dir.name.ifBlank { dir.pathString },
                url = dir.toUri().toURL(),
                fileLastModified = attrs.getUpdatedTime(),
              )

            return FileVisitResult.CONTINUE
          }

          override fun visitFile(
            file: Path,
            attrs: BasicFileAttributes,
          ): FileVisitResult {
            logger.trace { "visitFile: $file (regularFile:${attrs.isRegularFile}, directory:${attrs.isDirectory}, symbolicLink:${attrs.isSymbolicLink}, other:${attrs.isOther})" }
            if (!attrs.isSymbolicLink && !attrs.isDirectory) {
              if (scanForExtensions.contains(file.extension.lowercase()) &&
                !file.name.startsWith(".")
              ) {
                val book = pathToBook(file, attrs)
                file.parent.let { key ->
                  pathToBooks.merge(key, mutableListOf(book)) { prev, one -> prev.union(one).toMutableList() }
                }
              }

              val seriesSidecarConsumer =
                sidecarSeriesConsumers
                  .firstOrNull { consumer ->
                    consumer.getSidecarSeriesFilenames().any { file.name.equals(it, ignoreCase = true) }
                  }
              seriesSidecarConsumer?.let {
                val sidecar = Sidecar(file.toUri().toURL(), file.parent.toUri().toURL(), attrs.getUpdatedTime(), it.getSidecarSeriesType(), Sidecar.Source.SERIES)
                pathToSeriesSidecars.merge(file.parent, mutableListOf(sidecar)) { prev, one -> prev.union(one).toMutableList() }
              }

              if (scanCbx && file.isImageFile() && seriesSidecarConsumer == null) {
                imageFilesByDirectory.getOrPut(file.parent) { mutableListOf() }.add(file)
              }

              // book sidecars can't be exactly matched during a file visit
              // this prefilters files to reduce the candidates
              if (sidecarBookPrefilter.any { it.matches(file.name) }) {
                val sidecar = TempSidecar(file.name, file.toUri().toURL(), attrs.getUpdatedTime())
                pathToBookSidecars.merge(file.parent, mutableListOf(sidecar)) { prev, one -> prev.union(one).toMutableList() }
              }
            }

            return FileVisitResult.CONTINUE
          }

          override fun visitFileFailed(
            file: Path?,
            exc: IOException?,
          ): FileVisitResult {
            logger.warn { "Could not access: $file" }
            return FileVisitResult.SKIP_SUBTREE
          }

          override fun postVisitDirectory(
            dir: Path,
            exc: IOException?,
          ): FileVisitResult {
            logger.trace { "postVisit: $dir" }
            imageFilesByDirectory[dir]?.takeIf { it.isNotEmpty() }?.let {
              pathToBooks.getOrPut(dir) { mutableListOf() }.add(pathToDirectoryBook(dir))
            }
            val books = pathToBooks[dir]
            val tempSeries = pathToSeries[dir]
            if (!books.isNullOrEmpty() && tempSeries !== null) {
              if (!oneshotsDir.isNullOrBlank() && dir.pathString.contains(oneshotsDir, true)) {
                books.forEach { book ->
                  val series =
                    Series(
                      name = book.name,
                      url = book.url,
                      fileLastModified = book.fileLastModified,
                      oneshot = true,
                    )
                  scannedSeries[series] = listOf(book.copy(oneshot = true))
                }
              } else {
                val series =
                  if (forceDirectoryModifiedTime)
                    tempSeries.copy(fileLastModified = maxOf(tempSeries.fileLastModified, books.maxOf { it.fileLastModified }))
                  else
                    tempSeries

                scannedSeries[series] = books

                // only add series sidecars if series has books
                pathToSeriesSidecars[dir]?.let { scannedSidecars.addAll(it) }
              }

              // book sidecars are matched here, with the actual list of books
              books.forEach { book ->
                val sidecars =
                  pathToBookSidecars[dir]
                    ?.mapNotNull { sidecar ->
                      sidecarBookConsumers.firstOrNull { it.isSidecarBookMatch(book.name, sidecar.name) }?.let {
                        sidecar to it.getSidecarBookType()
                      }
                    }?.toMap() ?: emptyMap()
                pathToBookSidecars[dir]?.minusAssign(sidecars.keys)

                sidecars.mapTo(scannedSidecars) { (sidecar, type) ->
                  Sidecar(sidecar.url, book.url, sidecar.lastModifiedTime, type, Sidecar.Source.BOOK)
                }
              }
            }

            return FileVisitResult.CONTINUE
          }
        },
      )
    }.also {
      val countOfBooks = scannedSeries.values.sumOf { it.size }
      logger.info { "Scanned ${scannedSeries.size} series, $countOfBooks books, and ${scannedSidecars.size} sidecars in $it" }
    }

    return ScanResult(scannedSeries, scannedSidecars)
  }

  private fun scanTopLevelSeries(
    root: Path,
    scanForExtensions: Collection<String>,
    forceDirectoryModifiedTime: Boolean,
    oneshotsDir: String?,
    directoryExclusions: Set<String>,
    scanImageDirectories: Boolean,
  ): ScanResult {
    val booksBySeriesPath = mutableMapOf<Path, MutableList<Book>>()
    val seriesSidecarsByPath = mutableMapOf<Path, MutableList<Sidecar>>()
    val bookSidecarsByPath = mutableMapOf<Path, MutableList<TempSidecar>>()
    val imageFilesByDirectory = mutableMapOf<Path, MutableList<Path>>()

    measureTime {
      Files.walkFileTree(
        root,
        setOf(FileVisitOption.FOLLOW_LINKS),
        Integer.MAX_VALUE,
        object : FileVisitor<Path> {
          override fun preVisitDirectory(
            dir: Path,
            attrs: BasicFileAttributes,
          ): FileVisitResult {
            if (dir != root &&
              (dir.name.startsWith(".") || directoryExclusions.any { exclude -> dir.pathString.contains(exclude, true) })
            ) {
              return FileVisitResult.SKIP_SUBTREE
            }
            return FileVisitResult.CONTINUE
          }

          override fun visitFile(
            file: Path,
            attrs: BasicFileAttributes,
          ): FileVisitResult {
            if (attrs.isSymbolicLink || attrs.isDirectory || file.name.startsWith(".")) return FileVisitResult.CONTINUE

            val relative = root.relativize(file)
            val seriesPath = if (relative.nameCount == 1) root else root.resolve(relative.getName(0))

            if (scanForExtensions.contains(file.extension.lowercase())) {
              val directoryPath =
                seriesPath
                  .relativize(file.parent)
                  .map { it.toString() }
                  .joinToString("/")
              booksBySeriesPath
                .getOrPut(seriesPath) { mutableListOf() }
                .add(pathToBook(file, attrs).copy(directoryPath = directoryPath))
            }

            val seriesSidecarConsumer =
              if (file.parent == seriesPath) {
                sidecarSeriesConsumers
                  .firstOrNull { consumer -> consumer.getSidecarSeriesFilenames().any { file.name.equals(it, ignoreCase = true) } }
              } else {
                null
              }
            seriesSidecarConsumer?.let { consumer ->
              seriesSidecarsByPath
                .getOrPut(seriesPath) { mutableListOf() }
                .add(Sidecar(file.toUri().toURL(), seriesPath.toUri().toURL(), attrs.getUpdatedTime(), consumer.getSidecarSeriesType(), Sidecar.Source.SERIES))
            }

            if (scanImageDirectories && file.isImageFile() && seriesSidecarConsumer == null) {
              imageFilesByDirectory.getOrPut(file.parent) { mutableListOf() }.add(file)
            }

            if (sidecarBookPrefilter.any { it.matches(file.name) }) {
              bookSidecarsByPath
                .getOrPut(file.parent) { mutableListOf() }
                .add(TempSidecar(file.name, file.toUri().toURL(), attrs.getUpdatedTime()))
            }
            return FileVisitResult.CONTINUE
          }

          override fun visitFileFailed(
            file: Path?,
            exc: IOException?,
          ): FileVisitResult {
            logger.warn(exc) { "Could not access: $file" }
            return FileVisitResult.SKIP_SUBTREE
          }

          override fun postVisitDirectory(
            dir: Path,
            exc: IOException?,
          ): FileVisitResult {
            imageFilesByDirectory[dir]?.takeIf { it.isNotEmpty() }?.let {
              val seriesPath = dir.toTopLevelSeriesPath(root)
              val directoryPath =
                if (dir == seriesPath) {
                  ""
                } else {
                  seriesPath
                    .relativize(dir.parent)
                    .map { it.toString() }
                    .joinToString("/")
                }
              booksBySeriesPath
                .getOrPut(seriesPath) { mutableListOf() }
                .add(pathToDirectoryBook(dir).copy(directoryPath = directoryPath))
            }
            return FileVisitResult.CONTINUE
          }
        },
      )
    }.also { logger.info { "Scanned top-level series layout in $it" } }

    val scannedSeries = mutableMapOf<Series, List<Book>>()
    val scannedSidecars = mutableListOf<Sidecar>()
    booksBySeriesPath.forEach { (seriesPath, books) ->
      val attrs = seriesPath.readAttributes<BasicFileAttributes>()
      if (!oneshotsDir.isNullOrBlank() && seriesPath.pathString.contains(oneshotsDir, true)) {
        books.forEach { book ->
          scannedSeries[
            Series(
              name = book.name,
              url = book.url,
              fileLastModified = book.fileLastModified,
              oneshot = true,
            ),
          ] = listOf(book.copy(oneshot = true, directoryPath = ""))
        }
      } else {
        val seriesModified =
          if (forceDirectoryModifiedTime)
            maxOf(attrs.getUpdatedTime(), books.maxOf { it.fileLastModified })
          else
            attrs.getUpdatedTime()
        scannedSeries[
          Series(
            name = seriesPath.name.ifBlank { seriesPath.pathString },
            url = seriesPath.toUri().toURL(),
            fileLastModified = seriesModified,
          ),
        ] = books
        seriesSidecarsByPath[seriesPath]?.let(scannedSidecars::addAll)
      }

      books.groupBy { it.path.parent }.forEach { (parent, booksInDirectory) ->
        booksInDirectory.forEach { book ->
          val matched =
            bookSidecarsByPath[parent]
              ?.mapNotNull { sidecar ->
                sidecarBookConsumers.firstOrNull { it.isSidecarBookMatch(book.name, sidecar.name) }?.let { sidecar to it.getSidecarBookType() }
              }?.toMap()
              .orEmpty()
          bookSidecarsByPath[parent]?.minusAssign(matched.keys)
          matched.mapTo(scannedSidecars) { (sidecar, type) ->
            Sidecar(sidecar.url, book.url, sidecar.lastModifiedTime, type, Sidecar.Source.BOOK)
          }
        }
      }
    }

    logger.info { "Scanned ${scannedSeries.size} top-level series, ${scannedSeries.values.sumOf { it.size }} books, and ${scannedSidecars.size} sidecars" }
    return ScanResult(scannedSeries, scannedSidecars)
  }

  fun scanFile(path: Path): Book? {
    if (!path.exists()) return null

    if (path.isDirectory()) {
      val containsImages =
        path
          .listDirectoryEntries()
          .any { it.isRegularFile() && !it.name.startsWith(".") && it.isImageFile() }
      return if (containsImages) pathToDirectoryBook(path) else null
    }

    return pathToBook(path, path.readAttributes())
  }

  fun scanBookSidecars(path: Path): List<Sidecar> {
    val bookBaseName = path.nameWithoutExtension
    val parent = path.parent
    return parent
      .listDirectoryEntries()
      .filter { candidate -> sidecarBookPrefilter.any { it.matches(candidate.name) } }
      .mapNotNull { candidate ->
        sidecarBookConsumers.firstOrNull { it.isSidecarBookMatch(bookBaseName, candidate.name) }?.let {
          Sidecar(candidate.toUri().toURL(), parent.toUri().toURL(), candidate.readAttributes<BasicFileAttributes>().getUpdatedTime(), it.getSidecarBookType(), Sidecar.Source.BOOK)
        }
      }
  }

  private fun pathToBook(
    path: Path,
    attrs: BasicFileAttributes,
  ): Book =
    Book(
      name = path.nameWithoutExtension,
      url = path.toUri().toURL(),
      fileLastModified = attrs.getUpdatedTime(),
      fileSize = attrs.size(),
    )

  private fun pathToDirectoryBook(path: Path): Book {
    val attrs = path.readAttributes<BasicFileAttributes>()
    val files = path.listDirectoryEntries().filter { it.isRegularFile() && !it.name.startsWith(".") }
    val updatedTime = files.map { it.readAttributes<BasicFileAttributes>().getUpdatedTime() }.maxOrNull()
    return Book(
      name = path.name.ifBlank { path.pathString },
      url = path.toUri().toURL(),
      fileLastModified = maxOf(attrs.getUpdatedTime(), updatedTime ?: attrs.getUpdatedTime()),
      fileSize = files.sumOf { Files.size(it) },
    )
  }
}

private fun Path.isImageFile(): Boolean = extension.lowercase() in imageFileExtensions

private fun Path.toTopLevelSeriesPath(root: Path): Path {
  if (this == root) return root
  val relative = root.relativize(this)
  return root.resolve(relative.getName(0))
}

fun BasicFileAttributes.getUpdatedTime(): LocalDateTime = maxOf(creationTime(), lastModifiedTime()).toLocalDateTime()

fun FileTime.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(this.toInstant(), ZoneId.systemDefault())
