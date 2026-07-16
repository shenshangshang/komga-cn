package org.gotson.komga.domain.service

import io.mockk.mockk
import org.apache.tika.config.TikaConfig
import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.domain.model.Book
import org.gotson.komga.domain.model.BookWithMedia
import org.gotson.komga.domain.model.Media
import org.gotson.komga.domain.model.MediaType
import org.gotson.komga.infrastructure.configuration.KomgaSettingsProvider
import org.gotson.komga.infrastructure.hash.Hasher
import org.gotson.komga.infrastructure.image.ImageAnalyzer
import org.gotson.komga.infrastructure.image.ImageConverter
import org.gotson.komga.infrastructure.image.ImageType
import org.gotson.komga.infrastructure.image.QrCodeDetector
import org.gotson.komga.infrastructure.mediacontainer.ContentDetector
import org.gotson.komga.infrastructure.mediacontainer.divina.DirectoryExtractor
import org.gotson.komga.infrastructure.mediacontainer.epub.EpubExtractor
import org.gotson.komga.infrastructure.mediacontainer.mobi.MobiExtractor
import org.gotson.komga.infrastructure.mediacontainer.pdf.PdfExtractor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import javax.imageio.ImageIO

class BookAnalyzerDirectoryTest {
  private val contentDetector = ContentDetector(TikaConfig.getDefaultConfig())
  private val imageAnalyzer = ImageAnalyzer()
  private val directoryExtractor = DirectoryExtractor(contentDetector, imageAnalyzer)
  private val analyzer =
    BookAnalyzer(
      contentDetector = contentDetector,
      extractors = listOf(directoryExtractor),
      pdfExtractor = mockk(),
      epubExtractor = mockk<EpubExtractor>(),
      mobiExtractor = mockk<MobiExtractor>(),
      imageConverter = mockk<ImageConverter>(),
      imageAnalyzer = imageAnalyzer,
      qrCodeDetector = mockk<QrCodeDetector>(),
      hasher = Hasher(),
      pageHashing = 0,
      komgaSettingsProvider = mockk<KomgaSettingsProvider>(),
      thumbnailType = ImageType.PNG,
      pdfImageType = ImageType.PNG,
    )

  @Test
  fun `given image directory when analyzing then media is ready and pages can be read`(
    @TempDir directory: Path,
  ) {
    ImageIO.write(BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB), "png", directory.resolve("10.png").toFile())
    ImageIO.write(BufferedImage(20, 30, BufferedImage.TYPE_INT_RGB), "png", directory.resolve("2.png").toFile())
    val book = Book("folder book", directory.toUri().toURL(), LocalDateTime.now())

    val media = analyzer.analyze(book, true)

    assertThat(media.mediaType).isEqualTo(MediaType.DIRECTORY.type)
    assertThat(media.status).isEqualTo(Media.Status.READY)
    assertThat(media.pages.map { it.fileName }).containsExactly("2.png", "10.png")
    assertThat(analyzer.getPageContent(BookWithMedia(book, media), 1))
      .isEqualTo(Files.readAllBytes(directory.resolve("2.png")))
  }
}
