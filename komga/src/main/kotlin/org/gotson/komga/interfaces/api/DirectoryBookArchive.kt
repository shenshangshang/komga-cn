package org.gotson.komga.interfaces.api

import org.gotson.komga.infrastructure.util.NaturalSortComparator
import org.springframework.stereotype.Component
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.name

@Component
class DirectoryBookArchive {
  fun write(
    directory: Path,
    output: OutputStream,
  ) {
    require(Files.isDirectory(directory)) { "Path is not a directory: $directory" }

    ZipOutputStream(output).use { zip ->
      Files.list(directory).use { entries ->
        entries
          .filter { Files.isRegularFile(it, NOFOLLOW_LINKS) && !it.name.startsWith(".") }
          .sorted(compareBy(NaturalSortComparator) { it.name })
          .forEach { file ->
            zip.putNextEntry(ZipEntry(file.name))
            Files.newInputStream(file).use { it.copyTo(zip) }
            zip.closeEntry()
          }
      }
    }
  }
}
