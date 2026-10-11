package org.gotson.komga.infrastructure.metadata.barcode

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.validator.routines.ISBNValidator
import org.gotson.komga.domain.model.BookMetadataPatch
import org.gotson.komga.domain.model.BookMetadataPatchCapability
import org.gotson.komga.domain.model.BookWithMedia
import org.gotson.komga.domain.model.Library
import org.gotson.komga.domain.model.MediaProfile
import org.gotson.komga.domain.model.MetadataPatchTarget
import org.gotson.komga.domain.service.BookAnalyzer
import org.gotson.komga.infrastructure.metadata.BookMetadataProvider
import org.springframework.stereotype.Service
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.util.EnumSet
import javax.imageio.ImageIO
import kotlin.math.max

private val logger = KotlinLogging.logger {}

private const val PAGES_LAST = 3
private const val PAGES_FIRST = 3

// Barcodes stay readable when downscaled; cap the scanned page so the width*height
// pixel array cannot exhaust the heap on very large pages.
private const val BARCODE_MAX_EDGE = 2000

@Service
class IsbnBarcodeProvider(
  private val bookAnalyzer: BookAnalyzer,
  private val validator: ISBNValidator,
) : BookMetadataProvider {
  private val hints =
    mapOf(
      DecodeHintType.POSSIBLE_FORMATS to EnumSet.of(BarcodeFormat.EAN_13),
      DecodeHintType.TRY_HARDER to true,
    )

  override val capabilities = setOf(BookMetadataPatchCapability.ISBN)

  override fun getBookMetadataFromBook(book: BookWithMedia): BookMetadataPatch? {
    if (book.media.profile == MediaProfile.EPUB) return null

    val pagesToTry =
      (1..book.media.pageCount).toList().let {
        (it.takeLast(PAGES_LAST).reversed() + it.take(PAGES_FIRST)).distinct()
      }

    for (p in pagesToTry) {
      var image: java.awt.image.BufferedImage? = null
      try {
        val imageBytes = bookAnalyzer.getPageContent(book, p)
        image = ImageIO.read(imageBytes.inputStream())
        val full = image
        if (full != null) {
          val scan = full.downscaleForBarcode()
          try {
            val pixels = scan.getRGB(0, 0, scan.width, scan.height, null, 0, scan.width)
            val source = RGBLuminanceSource(scan.width, scan.height, pixels)
            val bitmap = BinaryBitmap(HybridBinarizer(source))

            val result =
              try {
                MultiFormatReader().decode(bitmap, hints)
              } catch (e: Exception) {
                null
              }

            if (result == null || result.text == null) {
              logger.debug { "Book page $p does not contain a barcode: $book" }
            } else {
              if (validator.isValid(result.text)) {
                logger.debug { "Book page $p contains barcode which is valid ISBN: '${result.text}'. $book" }
                return BookMetadataPatch(isbn = validator.validate(result.text))
              } else {
                logger.debug { "Book page $p contains barcode which is invalid ISBN: '${result.text}'. $book" }
              }
            }
          } finally {
            if (scan !== full) scan.flush()
          }
        }
      } catch (e: Exception) {
        logger.error(e) { "Error while processing page" }
      } finally {
        // Manual cleanup to prevent memory leaks
        image?.flush()
        image = null
      }
    }

    return null
  }

  /** Return this image, downscaled to at most [BARCODE_MAX_EDGE] on its longest edge. */
  private fun BufferedImage.downscaleForBarcode(): BufferedImage {
    val longestEdge = max(width, height)
    if (longestEdge <= BARCODE_MAX_EDGE) return this
    val scale = BARCODE_MAX_EDGE.toDouble() / longestEdge
    val targetWidth = max(1, (width * scale).toInt())
    val targetHeight = max(1, (height * scale).toInt())
    val scaled = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
    val graphics = scaled.createGraphics()
    try {
      graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
      graphics.drawImage(this, 0, 0, targetWidth, targetHeight, null)
    } finally {
      graphics.dispose()
    }
    return scaled
  }

  override fun shouldLibraryHandlePatch(
    library: Library,
    target: MetadataPatchTarget,
  ): Boolean =
    when (target) {
      MetadataPatchTarget.BOOK -> library.importBarcodeIsbn
      else -> false
    }
}
