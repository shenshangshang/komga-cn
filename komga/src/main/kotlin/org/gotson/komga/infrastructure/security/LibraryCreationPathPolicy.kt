package org.gotson.komga.infrastructure.security

import org.gotson.komga.domain.model.KomgaUser
import org.gotson.komga.infrastructure.configuration.KomgaSettingsProvider
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Component
class LibraryCreationPathPolicy(
  private val settings: KomgaSettingsProvider,
) {
  fun allowedRoots(): List<Path> =
    settings.libraryCreationAllowedRoots
      .mapNotNull { configured ->
        runCatching { Paths.get(configured).toRealPath() }
          .getOrNull()
          ?.takeIf { Files.isDirectory(it) }
      }.distinct()

  fun requireLibraryRootAllowed(
    user: KomgaUser,
    requestedPath: Path,
  ): Path {
    if (user.isAdmin) return requestedPath
    val roots = allowedRoots()
    val normalized = requestedPath.toAbsolutePath().normalize()
    if (roots.none { normalized.startsWith(it) && normalized != it }) throw forbidden()
    val realPath = requestedPath.toExistingRealPath()
    if (roots.none { realPath.startsWith(it) && realPath != it }) throw forbidden()
    return realPath
  }

  fun requireBrowseDirectoryAllowed(requestedPath: Path): Path {
    val roots = allowedRoots()
    return requireBrowseDirectoryAllowed(requestedPath, roots)
  }

  fun requireBrowseDirectoryAllowed(
    requestedPath: Path,
    roots: List<Path>,
  ): Path {
    val normalized = requestedPath.toAbsolutePath().normalize()
    if (roots.none { normalized.startsWith(it) }) throw forbidden()
    val realPath = requestedPath.toExistingRealPath()
    val directory = if (Files.isDirectory(realPath)) realPath else realPath.parent ?: throw forbidden()
    if (roots.none { directory.startsWith(it) }) throw forbidden()
    return directory
  }

  fun isBrowseable(
    path: Path,
    roots: List<Path>,
  ): Boolean {
    val normalized = path.toAbsolutePath().normalize()
    if (roots.none { normalized.startsWith(it) }) return false
    val realPath = runCatching { path.toRealPath() }.getOrNull() ?: return false
    return roots.any { realPath.startsWith(it) }
  }

  fun parentFor(
    directory: Path,
    roots: List<Path>,
  ): Path? {
    if (roots.any { directory == it }) return null
    return directory.parent?.takeIf { parent -> roots.any { parent.startsWith(it) } }
  }

  private fun Path.toExistingRealPath(): Path =
    runCatching { toRealPath() }
      .getOrElse { throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Path does not exist") }

  private fun forbidden() = ResponseStatusException(HttpStatus.FORBIDDEN, "Path is outside the allowed library roots")
}
