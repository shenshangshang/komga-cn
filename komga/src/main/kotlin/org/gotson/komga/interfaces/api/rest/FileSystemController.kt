package org.gotson.komga.interfaces.api.rest

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.gotson.komga.infrastructure.openapi.OpenApiConfiguration
import org.gotson.komga.infrastructure.security.KomgaPrincipal
import org.gotson.komga.infrastructure.security.LibraryCreationPathPolicy
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

@RestController
@RequestMapping("api/v1/filesystem", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ADMIN', 'CREATE_LIBRARY')")
@Tag(name = OpenApiConfiguration.TagNames.FILE_SYSTEM)
class FileSystemController(
  private val libraryCreationPathPolicy: LibraryCreationPathPolicy,
) {
  private val fs = FileSystems.getDefault()

  @PostMapping
  @Operation(
    summary = "Directory listing",
    description = "List folders and files from the host server's file system. If no request body is passed then the root directories are returned.",
  )
  fun getDirectoryListing(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @RequestBody(required = false) request: DirectoryRequestDto = DirectoryRequestDto(),
  ): DirectoryListingDto {
    val isAdmin = principal.user.isAdmin
    val showFiles = request.showFiles && isAdmin
    val allowedRoots = if (isAdmin) emptyList() else libraryCreationPathPolicy.allowedRoots()
    if (request.path.isEmpty()) {
      return DirectoryListingDto(
        directories = (if (isAdmin) fs.rootDirectories else allowedRoots).map { it.toDto() },
        files = emptyList(),
      )
    }

    val requestedPath = fs.getPath(request.path)
    if (!requestedPath.isAbsolute) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Path must be absolute")
    val directory =
      if (isAdmin) {
        if (!Files.exists(requestedPath)) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Path does not exist")
        if (Files.isDirectory(requestedPath)) requestedPath else requestedPath.parent
      } else {
        libraryCreationPathPolicy.requireBrowseDirectoryAllowed(requestedPath, allowedRoots)
      }

    try {
      val (directories, files) =
        Files.list(directory).use { dirStream ->
          dirStream
            .asSequence()
            .filter { !Files.isHidden(it) && (if (!showFiles) Files.isDirectory(it) else true) }
            .filter { isAdmin || libraryCreationPathPolicy.isBrowseable(it, allowedRoots) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.toString() })
            .map { it.toDto() }
            .toList()
            .partition { it.type == "directory" }
        }
      return DirectoryListingDto(
        parent = if (isAdmin) directory.parent?.toString() else libraryCreationPathPolicy.parentFor(directory, allowedRoots)?.toString(),
        directories = directories,
        files = files,
      )
    } catch (e: ResponseStatusException) {
      throw e
    } catch (e: Exception) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Path does not exist")
    }
  }
}

data class DirectoryRequestDto(
  val path: String = "",
  val showFiles: Boolean = false,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DirectoryListingDto(
  val parent: String? = null,
  val directories: List<PathDto>,
  val files: List<PathDto>,
)

data class PathDto(
  val type: String,
  val name: String,
  val path: String,
)

fun Path.toDto(): PathDto =
  PathDto(
    type = if (Files.isDirectory(this)) "directory" else "file",
    name = (fileName ?: this).toString(),
    path = toString(),
  )
