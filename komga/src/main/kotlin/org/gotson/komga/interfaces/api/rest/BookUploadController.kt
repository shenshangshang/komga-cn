package org.gotson.komga.interfaces.api.rest

import org.gotson.komga.domain.model.CopyMode
import org.gotson.komga.domain.persistence.SeriesRepository
import org.gotson.komga.domain.service.BookImporter
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
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.deleteIfExists

@RestController
class BookUploadController(
  private val seriesRepository: SeriesRepository,
  private val bookImporter: BookImporter,
) {
  @PostMapping("api/v1/books/upload")
  @PreAuthorize("hasAnyRole('ADMIN', 'UPLOAD_BOOK')")
  @ResponseStatus(HttpStatus.CREATED)
  fun upload(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @RequestParam seriesId: String,
    @RequestParam file: MultipartFile,
  ) {
    val series = seriesRepository.findByIdOrNull(seriesId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    if (!principal.user.canAccessLibrary(series.libraryId)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
    if (series.oneshot) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploads to one-shot series are not supported")

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

  private companion object {
    const val MAX_UPLOAD_BYTES = 1024L * 1024L * 1024L
    val ALLOWED_EXTENSIONS = setOf("cbz", "cbr", "zip", "rar", "7z")
  }
}
