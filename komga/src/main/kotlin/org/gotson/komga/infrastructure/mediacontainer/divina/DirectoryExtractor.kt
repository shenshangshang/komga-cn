package org.gotson.komga.infrastructure.mediacontainer.divina

import io.github.oshai.kotlinlogging.KotlinLogging
import org.gotson.komga.domain.model.MediaContainerEntry
import org.gotson.komga.domain.model.MediaType
import org.gotson.komga.infrastructure.image.ImageAnalyzer
import org.gotson.komga.infrastructure.mediacontainer.ContentDetector
import org.gotson.komga.infrastructure.util.NaturalSortComparator
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

@Service
class DirectoryExtractor(
  private val contentDetector: ContentDetector,
  private val imageAnalyzer: ImageAnalyzer,
) : DivinaExtractor {
  override fun mediaTypes(): List<String> = listOf(MediaType.DIRECTORY.type)

  override fun getEntries(
    path: Path,
    analyzeDimensions: Boolean,
  ): List<MediaContainerEntry> {
    require(path.isDirectory()) { "Path is not a directory: $path" }

    return path
      .listDirectoryEntries()
      .filter { it.isRegularFile() && !it.name.startsWith(".") }
      .parallelStream()
      .map { entry ->
        try {
          val mediaType = contentDetector.detectMediaTypeFast(entry)
          val dimension =
            if (analyzeDimensions && contentDetector.isImage(mediaType)) {
              Files.newInputStream(entry).buffered().use(imageAnalyzer::getDimension)
            } else {
              null
            }
          MediaContainerEntry(
            name = entry.name,
            mediaType = mediaType,
            dimension = dimension,
            fileSize = Files.size(entry),
          )
        } catch (e: Exception) {
          logger.warn(e) { "Could not analyze directory entry: $entry" }
          MediaContainerEntry(name = entry.name, comment = e.message)
        }
      }
      .toList()
      .sortedWith(compareBy(NaturalSortComparator) { it.name })
  }

  override fun getEntryStream(
    path: Path,
    entryName: String,
  ): ByteArray {
    require(path.isDirectory()) { "Path is not a directory: $path" }
    val entryPath = path.fileSystem.getPath(entryName)
    require(!entryPath.isAbsolute && entryPath.nameCount == 1) { "Invalid directory entry: $entryName" }

    val root = path.toRealPath()
    val entry = path.resolve(entryPath).toRealPath()
    require(entry.parent == root && entry.isRegularFile()) { "Invalid directory entry: $entryName" }
    return Files.readAllBytes(entry)
  }
}
