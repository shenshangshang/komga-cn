package org.gotson.komga.infrastructure.mediacontainer.av

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.gotson.komga.domain.model.MediaExtensionAudioVideo
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

@Service
class VideoMediaAnalyzer(
  private val objectMapper: ObjectMapper,
) {
  data class ProbeResult(
    val duration: Double?,
    val width: Int?,
    val height: Int?,
  )

  @JsonIgnoreProperties(ignoreUnknown = true)
  private data class FfprobeOutput(
    val streams: List<Stream>? = null,
    val format: Format? = null,
  )

  @JsonIgnoreProperties(ignoreUnknown = true)
  private data class Stream(
    val codecType: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: String? = null,
  )

  @JsonIgnoreProperties(ignoreUnknown = true)
  private data class Format(
    val duration: String? = null,
  )

  fun probe(path: Path): ProbeResult? {
    val process: Process
    val output: String
    try {
      process = ProcessBuilder(
        "ffprobe",
        "-v", "quiet",
        "-print_format", "json",
        "-show_format",
        "-show_streams",
        path.toString(),
      ).redirectErrorStream(true).start()

      output = process.inputStream.bufferedReader().use { it.readText() }
      if (!process.waitFor(30, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        logger.warn { "ffprobe timed out for: $path" }
        return null
      }
      if (process.exitValue() != 0) {
        logger.warn { "ffprobe exited with ${process.exitValue()} for: $path" }
        return null
      }
    } catch (e: Exception) {
      logger.warn(e) { "Failed to run ffprobe on: $path" }
      return null
    }

    return try {
      val parsed = objectMapper.readValue(output, FfprobeOutput::class.java)
      val videoStream = parsed.streams?.firstOrNull { it.codecType == "video" }
      val duration = parsed.format?.duration?.toDoubleOrNull()
        ?: videoStream?.duration?.toDoubleOrNull()
      ProbeResult(
        duration = duration,
        width = videoStream?.width,
        height = videoStream?.height,
      )
    } catch (e: Exception) {
      logger.warn(e) { "Failed to parse ffprobe output for: $path" }
      null
    }
  }

  fun toExtension(path: Path): MediaExtensionAudioVideo? {
    val result = probe(path) ?: return null
    return MediaExtensionAudioVideo(
      duration = result.duration,
      width = result.width,
      height = result.height,
    )
  }

  fun extractFrame(path: Path, timeOffset: Double = 1.0): ByteArray? {
    val process: Process
    try {
      process = ProcessBuilder(
        "ffmpeg",
        "-v", "quiet",
        "-ss", String.format("%.2f", timeOffset),
        "-i", path.toString(),
        "-frames:v", "1",
        "-f", "image2",
        "-c:v", "mjpeg",
        "-",
      ).redirectErrorStream(false).start()

      val bytes = process.inputStream.use { it.readBytes() }
      process.errorStream.close()
      if (!process.waitFor(30, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        logger.warn { "ffmpeg frame extraction timed out for: $path" }
        return null
      }
      if (process.exitValue() != 0 || bytes.isEmpty()) {
        logger.warn { "ffmpeg frame extraction failed (exit ${process.exitValue()}) for: $path" }
        return null
      }
      return bytes
    } catch (e: Exception) {
      logger.warn(e) { "Failed to extract frame from: $path" }
      return null
    }
  }

  fun extractEmbeddedCover(path: Path): ByteArray? {
    val process: Process
    try {
      // Try to extract an embedded cover art stream (attached pic / album art)
      process = ProcessBuilder(
        "ffmpeg",
        "-v", "quiet",
        "-i", path.toString(),
        "-map", "0:v:0",
        "-frames:v", "1",
        "-f", "image2",
        "-c:v", "mjpeg",
        "-",
      ).redirectErrorStream(false).start()

      val bytes = process.inputStream.use { it.readBytes() }
      process.errorStream.close()
      if (!process.waitFor(15, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        return null
      }
      return if (process.exitValue() == 0 && bytes.isNotEmpty()) bytes else null
    } catch (e: Exception) {
      logger.debug(e) { "No embedded cover found in: $path" }
      return null
    }
  }
}
