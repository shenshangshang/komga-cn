package org.gotson.komga.infrastructure.image

import io.github.oshai.kotlinlogging.KotlinLogging
import net.coobird.thumbnailator.Thumbnails
import org.gotson.komga.infrastructure.mediacontainer.ContentDetector
import org.springframework.stereotype.Service
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO
import javax.imageio.spi.IIORegistry
import javax.imageio.spi.ImageReaderSpi
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

private const val WEBP_NIGHT_MONKEYS = "com.github.gotson.nightmonkeys.webp.imageio.plugins.WebpImageReaderSpi"

// Only bother subsampling when the source is at least this many times larger than the target.
private const val SUBSAMPLE_MIN_FACTOR = 4

@Service
class ImageConverter(
  private val imageAnalyzer: ImageAnalyzer,
  private val contentDetector: ContentDetector,
) {
  val supportedReadFormats by lazy { ImageIO.getReaderFormatNames().toList() }
  val supportedReadMediaTypes by lazy { ImageIO.getReaderMIMETypes().toList() }
  val supportedWriteFormats by lazy { ImageIO.getWriterFormatNames().toList() }
  val supportedWriteMediaTypes by lazy { ImageIO.getWriterMIMETypes().toList() }

  init {
    chooseWebpReader()
    logger.info { "Supported read formats: $supportedReadFormats" }
    logger.info { "Supported read mediaTypes: $supportedReadMediaTypes" }
    logger.info { "Supported write formats: $supportedWriteFormats" }
    logger.info { "Supported write mediaTypes: $supportedWriteMediaTypes" }
  }

  private fun chooseWebpReader() {
    val providers =
      IIORegistry
        .getDefaultInstance()
        .getServiceProviders(
          ImageReaderSpi::class.java,
          { it is ImageReaderSpi && it.mimeTypes.contains("image/webp") },
          false,
        ).asSequence()
        .toList()

    if (providers.size > 1) {
      logger.debug { "WebP reader providers: ${providers.map { it.javaClass.canonicalName }}" }
      providers.firstOrNull { it.javaClass.canonicalName == WEBP_NIGHT_MONKEYS }?.let { nightMonkeys ->
        (providers - nightMonkeys).forEach {
          logger.debug { "Deregister provider: ${it.javaClass.canonicalName}" }
          IIORegistry.getDefaultInstance().deregisterServiceProvider(it)
        }
      }
    }
  }

  private val supportsTransparency = listOf("png")

  fun canConvertMediaType(
    from: String,
    to: String,
  ) = supportedReadMediaTypes.contains(from) && supportedWriteMediaTypes.contains(to)

  fun convertImage(
    imageBytes: ByteArray,
    format: String,
  ): ByteArray =
    ByteArrayOutputStream().use { baos ->
      var image: BufferedImage? = null
      var result: BufferedImage? = null

      try {
        image = ImageIO.read(imageBytes.inputStream())

        result =
          if (!supportsTransparency.contains(format) && containsAlphaChannel(image)) {
            if (containsTransparency(image))
              logger.info { "Image contains alpha channel but is not opaque, visual artifacts may appear" }
            else
              logger.info { "Image contains alpha channel but is opaque, conversion should not generate any visual artifacts" }
            BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB).also {
              it.createGraphics().drawImage(image, 0, 0, Color.WHITE, null)
            }
          } else {
            image
          }

        ImageIO.write(result, format, baos)
        baos.toByteArray()
      } finally {
        // Manual cleanup to prevent memory leaks
        if (result !== image) { // Only flush if result is a new image
          result?.flush()
        }
        image?.flush()
        result = null
        image = null
      }
    }

  fun resizeImageToByteArray(
    imageBytes: ByteArray,
    format: ImageType,
    size: Int,
  ): ByteArray {
    val builder = resizeImageBuilder(imageBytes, format, size) ?: return imageBytes

    return ByteArrayOutputStream().use {
      builder.toOutputStream(it)
      it.toByteArray()
    }
  }

  fun resizeImageToBufferedImage(
    imageBytes: ByteArray,
    format: ImageType,
    size: Int,
  ): BufferedImage {
    val builder = resizeImageBuilder(imageBytes, format, size)
    if (builder == null) {
      // Fallback: read image directly, but ensure cleanup
      var image: BufferedImage? = null
      try {
        image = ImageIO.read(imageBytes.inputStream())
        return image
      } catch (e: Exception) {
        image?.flush()
        throw e
      }
    }

    return builder.asBufferedImage()
  }

  private fun resizeImageBuilder(
    imageBytes: ByteArray,
    format: ImageType,
    size: Int,
  ): Thumbnails.Builder<*>? {
    val longestEdge =
      imageAnalyzer.getDimension(imageBytes.inputStream())?.let {
        val mediaType = contentDetector.detectMediaType(imageBytes.inputStream())
        val longestEdge = max(it.height, it.width)
        // don't resize if source and target format is the same, and source is smaller than desired
        if (mediaType == format.mediaType && longestEdge <= size) return null
        longestEdge
      }

    // prevent upscaling
    val resizeTo = if (longestEdge != null) min(longestEdge, size) else size

    // Reading a very large page fully into memory can exhaust the heap and abort book analysis.
    // When the source is much bigger than the target, decode it subsampled (down to ~2x the
    // target) so peak memory stays bounded; otherwise decode as before.
    val subsampled = longestEdge?.let { decodeSubsampled(imageBytes, it, resizeTo) }
    val builder: Thumbnails.Builder<*> =
      if (subsampled != null) {
        Thumbnails.of(subsampled)
      } else {
        Thumbnails.of(imageBytes.inputStream())
      }

    return builder
      .size(resizeTo, resizeTo)
      .imageType(BufferedImage.TYPE_INT_ARGB)
      .outputFormat(format.imageIOFormat)
  }

  /**
   * Decode [imageBytes] with source subsampling when its longest edge exceeds a safe multiple of
   * [target]. Returns null when the image is small enough to decode as-is, or when the reader does
   * not support subsampling.
   */
  private fun decodeSubsampled(
    imageBytes: ByteArray,
    sourceLongestEdge: Int,
    target: Int,
  ): BufferedImage? {
    if (sourceLongestEdge <= target * SUBSAMPLE_MIN_FACTOR) return null
    val subsample = max(1, sourceLongestEdge / (target * 2))
    if (subsample <= 1) return null

    return try {
      ImageIO.createImageInputStream(imageBytes.inputStream()).use { iis ->
        val readers = ImageIO.getImageReaders(iis)
        if (!readers.hasNext()) return null
        val reader = readers.next()
        try {
          reader.setInput(iis, true, true)
          val param = reader.defaultReadParam
          param.setSourceSubsampling(subsample, subsample, 0, 0)
          reader.read(0, param)
        } finally {
          reader.dispose()
        }
      }
    } catch (e: Exception) {
      logger.warn(e) { "Subsampled decode failed, falling back to full decode" }
      null
    }
  }

  private fun containsAlphaChannel(image: BufferedImage): Boolean = image.colorModel.hasAlpha()

  private fun containsTransparency(image: BufferedImage): Boolean {
    for (x in 0 until image.width) {
      for (y in 0 until image.height) {
        val pixel = image.getRGB(x, y)
        if (pixel shr 24 == 0x00) return true
      }
    }
    return false
  }
}
