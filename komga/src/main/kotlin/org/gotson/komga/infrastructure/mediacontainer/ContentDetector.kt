package org.gotson.komga.infrastructure.mediacontainer

import org.apache.tika.config.TikaConfig
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.Metadata
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name

private val extensionToMediaType = mapOf(
  "jpg" to "image/jpeg",
  "jpeg" to "image/jpeg",
  "png" to "image/png",
  "gif" to "image/gif",
  "bmp" to "image/bmp",
  "webp" to "image/webp",
  "tiff" to "image/tiff",
  "tif" to "image/tiff",
  "avif" to "image/avif",
  "jxl" to "image/jxl",
)

@Service
class ContentDetector(
  private val tika: TikaConfig,
) {
  fun detectMediaType(path: Path): String {
    val metadata =
      Metadata().also {
        it[Metadata.TIKA_MIME_FILE] = path.name
      }

    return TikaInputStream.get(path).use {
      val mediaType = tika.detector.detect(it, metadata)
      mediaType.toString()
    }
  }

  /**
   * Fast media type detection: uses file extension for common image formats
   * to avoid expensive Tika content sniffing (which reads file content over
   * network filesystems). Falls back to full Tika detection for unknown
   * extensions.
   */
  fun detectMediaTypeFast(path: Path): String =
    extensionToMediaType[path.extension.lowercase()]
      ?: detectMediaType(path)

  /**
   * Detects the media type of the content of the stream.
   * The stream will not be closed.
   */
  fun detectMediaType(stream: InputStream): String = tika.detector.detect(stream, Metadata()).toString()

  fun isImage(mediaType: String): Boolean = mediaType.startsWith("image/")

  fun mediaTypeToExtension(mediaType: String): String? =
    try {
      tika.mimeRepository.forName(mediaType).extension
    } catch (e: Exception) {
      null
    }
}
