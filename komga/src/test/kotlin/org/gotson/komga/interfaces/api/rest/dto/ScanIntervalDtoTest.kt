package org.gotson.komga.interfaces.api.rest.dto

import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.domain.model.Library
import org.junit.jupiter.api.Test

class ScanIntervalDtoTest {
  @Test
  fun `new libraries default to every 15 minutes`() {
    val domain = Library("library", java.net.URL("file:/library"))
    val creationDto = LibraryCreationDto(name = "library", root = "/library")

    assertThat(domain.scanInterval).isEqualTo(Library.ScanInterval.EVERY_15M)
    assertThat(creationDto.scanInterval).isEqualTo(ScanIntervalDto.EVERY_15M)
  }

  @Test
  fun `every 15 minutes maps between dto and domain`() {
    assertThat(ScanIntervalDto.EVERY_15M.toDomain()).isEqualTo(Library.ScanInterval.EVERY_15M)
    assertThat(Library.ScanInterval.EVERY_15M.toDto()).isEqualTo(ScanIntervalDto.EVERY_15M)
  }
}
