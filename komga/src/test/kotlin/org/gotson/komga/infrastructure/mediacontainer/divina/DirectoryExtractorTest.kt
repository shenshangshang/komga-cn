package org.gotson.komga.infrastructure.mediacontainer.divina

import org.apache.tika.config.TikaConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gotson.komga.infrastructure.image.ImageAnalyzer
import org.gotson.komga.infrastructure.mediacontainer.ContentDetector
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

class DirectoryExtractorTest {
  private val extractor = DirectoryExtractor(ContentDetector(TikaConfig.getDefaultConfig()), ImageAnalyzer())

  @Test
  fun `given image directory when extracting then return naturally sorted image pages`(
    @TempDir directory: Path,
  ) {
    writePng(directory.resolve("10.png"), 10, 20)
    writePng(directory.resolve("2.png"), 20, 30)
    Files.writeString(directory.resolve("ComicInfo.xml"), "<ComicInfo/>")

    val entries = extractor.getEntries(directory, analyzeDimensions = true)

    assertThat(entries.map { it.name }).containsExactly("2.png", "10.png", "ComicInfo.xml")
    assertThat(entries.take(2).map { it.mediaType }).containsOnly("image/png")
    assertThat(entries[0].dimension?.width).isEqualTo(20)
    assertThat(entries[0].dimension?.height).isEqualTo(30)
    assertThat(extractor.getEntryStream(directory, "2.png")).isEqualTo(Files.readAllBytes(directory.resolve("2.png")))
  }

  @Test
  fun `given traversal entry name when reading then reject it`(
    @TempDir directory: Path,
  ) {
    assertThatThrownBy { extractor.getEntryStream(directory, "../outside.png") }
      .isInstanceOf(IllegalArgumentException::class.java)
  }

  private fun writePng(
    path: Path,
    width: Int,
    height: Int,
  ) {
    ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", path.toFile())
  }
}
