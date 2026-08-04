package org.gotson.komga.interfaces.api.rest

import org.gotson.komga.domain.model.CopyMode
import org.gotson.komga.domain.model.Series
import org.gotson.komga.domain.persistence.LibraryRepository
import org.gotson.komga.domain.persistence.SeriesRepository
import org.gotson.komga.domain.service.BookImporter
import org.gotson.komga.domain.service.SeriesLifecycle
import org.gotson.komga.infrastructure.security.KomgaPrincipal
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import kotlin.io.path.deleteIfExists
import kotlin.io.path.name

@RestController
class BookUploadController(
  private val libraryRepository: LibraryRepository,
  private val seriesRepository: SeriesRepository,
  private val seriesLifecycle: SeriesLifecycle,
  private val bookImporter: BookImporter,
) {
  @PostMapping("api/v1/books/upload")
  @PreAuthorize("hasAnyRole('ADMIN', 'UPLOAD_BOOK')")
  @ResponseStatus(HttpStatus.CREATED)
  fun upload(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @RequestParam(required = false) seriesId: String?,
    @RequestParam(required = false) libraryId: String?,
    @RequestParam(required = false) seriesName: String?,
    @RequestParam file: MultipartFile,
  ) {
    val originalName =
      file.originalFilename?.let { Paths.get(it).fileName.toString() }
        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A file name is required")
    val extension = originalName.substringAfterLast('.', "").lowercase()
    if (extension !in ALLOWED_EXTENSIONS) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported archive type")
    }
    if (file.isEmpty || file.size > MAX_UPLOAD_BYTES) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Archive is empty or exceeds 1 GiB")
    }

    val temporary = Files.createTempFile("komga-upload-", ".$extension")
    try {
      file.transferTo(temporary)
      val series = resolveTargetSeries(principal, seriesId, libraryId, seriesName)
      bookImporter.importBook(
        sourceFile = temporary,
        series = series,
        copyMode = CopyMode.COPY,
        destinationName = originalName.substringBeforeLast('.'),
      )
    } finally {
      temporary.deleteIfExists()
    }
  }

  private fun resolveTargetSeries(
    principal: KomgaPrincipal,
    seriesId: String?,
    libraryId: String?,
    seriesName: String?,
  ): Series {
    val normalizedSeriesId = seriesId?.trim().orEmpty()
    val normalizedSeriesName = seriesName?.trim().orEmpty()
    if ((normalizedSeriesId.isEmpty()) == (normalizedSeriesName.isEmpty())) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose either an existing series or a new series name")
    }

    if (normalizedSeriesId.isNotEmpty()) {
      val series = seriesRepository.findByIdOrNull(normalizedSeriesId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
      if (!principal.user.canAccessLibrary(series.libraryId)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
      if (!libraryId.isNullOrBlank() && libraryId.trim() != series.libraryId) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "The series does not belong to the selected library")
      }
      if (series.oneshot) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploads to one-shot series are not supported")
      return series
    }

    val normalizedLibraryId = libraryId?.trim().orEmpty()
    if (normalizedLibraryId.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A library is required for a new series")
    val library = libraryRepository.findByIdOrNull(normalizedLibraryId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    if (!principal.user.canAccessLibrary(library.id)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
    validateNewSeriesName(seriesName.orEmpty(), normalizedSeriesName)

    val libraryPath = library.path.toAbsolutePath().normalize()
    val seriesPath = libraryPath.resolve(normalizedSeriesName).normalize()
    if (seriesPath.parent != libraryPath) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "The series name must be a single directory name")
    if (library.scanDirectoryExclusions.any { seriesPath.toString().contains(it, ignoreCase = true) }) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "The series name matches a library scan exclusion")
    }

    try {
      Files.createDirectory(seriesPath)
    } catch (_: FileAlreadyExistsException) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "A series directory with this name already exists")
    }

    return try {
      seriesLifecycle.createSeries(
        Series(
          name = seriesPath.name,
          url = seriesPath.toUri().toURL(),
          fileLastModified = LocalDateTime.now(),
          libraryId = library.id,
        ),
      )
    } catch (e: Exception) {
      seriesPath.deleteIfExists()
      throw e
    }
  }

  private fun validateNewSeriesName(
    rawName: String,
    normalizedName: String,
  ) {
    if (rawName != normalizedName || normalizedName.isEmpty()) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "The series name cannot be blank or contain surrounding whitespace")
    }
    if (normalizedName.length > MAX_SERIES_NAME_LENGTH ||
      normalizedName.startsWith('.') ||
      normalizedName.endsWith('.') ||
      invalidSeriesNameCharacters.containsMatchIn(normalizedName) ||
      windowsReservedSeriesNames.matches(normalizedName)
    ) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "The series name is not a valid portable directory name")
    }
  }

  private companion object {
    const val MAX_UPLOAD_BYTES = 1024L * 1024L * 1024L
    const val MAX_SERIES_NAME_LENGTH = 200
    val ALLOWED_EXTENSIONS = setOf("cbz", "cbr", "zip", "rar", "7z")
    val invalidSeriesNameCharacters = Regex("""[<>:"/\\|?*\u0000-\u001f]""")
    val windowsReservedSeriesNames = Regex("""(?i)(con|prn|aux|nul|com[1-9]|lpt[1-9])(\..*)?""")
  }
}
