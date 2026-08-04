package org.gotson.komga.interfaces.api.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.gotson.komga.domain.service.LibraryLifecycle
import org.gotson.komga.infrastructure.configuration.KomgaSettingsProvider
import org.gotson.komga.interfaces.api.rest.dto.LibraryCreationDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.nio.file.Files
import java.nio.file.Path

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class LibraryCreationAuthorizationTest(
  @Autowired private val mockMvc: MockMvc,
  @Autowired private val settings: KomgaSettingsProvider,
  @Autowired private val objectMapper: ObjectMapper,
) {
  @MockkBean
  private lateinit var libraryLifecycle: LibraryLifecycle

  @TempDir
  private lateinit var parent: Path

  private lateinit var allowedRoot: Path
  private lateinit var allowedLibrary: Path
  private lateinit var outsideLibrary: Path

  @BeforeEach
  fun setup() {
    allowedRoot = Files.createDirectory(parent.resolve("allowed"))
    allowedLibrary = Files.createDirectory(allowedRoot.resolve("library"))
    outsideLibrary = Files.createDirectory(parent.resolve("outside"))
    settings.libraryCreationAllowedRoots = listOf(allowedRoot.toString())
    every { libraryLifecycle.addLibrary(any()) } answers { firstArg() }
  }

  @AfterEach
  fun cleanup() {
    settings.libraryCreationAllowedRoots = emptyList()
    clearMocks(libraryLifecycle)
  }

  @Test
  @WithMockCustomUser(roles = ["CREATE_LIBRARY"])
  fun `library creator can create a library below an allowed root`() {
    postLibrary(allowedLibrary).andExpect { status { isOk() } }

    verify(exactly = 1) { libraryLifecycle.addLibrary(match { it.path == allowedLibrary.toRealPath() }) }
  }

  @Test
  @WithMockCustomUser(roles = ["CREATE_LIBRARY"])
  fun `library creator symlink path is persisted as its canonical target`() {
    val target = Files.createDirectory(allowedRoot.resolve("canonical"))
    val link = allowedRoot.resolve("link")
    assumeTrue(runCatching { Files.createSymbolicLink(link, target) }.isSuccess)

    postLibrary(link).andExpect { status { isOk() } }

    verify(exactly = 1) { libraryLifecycle.addLibrary(match { it.path == target.toRealPath() }) }
  }

  @Test
  @WithMockCustomUser(roles = ["CREATE_LIBRARY"])
  fun `library creator cannot use the allowed root itself or an outside directory`() {
    postLibrary(allowedRoot).andExpect { status { isForbidden() } }
    postLibrary(outsideLibrary).andExpect { status { isForbidden() } }

    verify(exactly = 0) { libraryLifecycle.addLibrary(any()) }
  }

  @Test
  @WithMockCustomUser(roles = ["ADMIN"])
  fun `administrator can create a library outside configured roots`() {
    postLibrary(outsideLibrary).andExpect { status { isOk() } }

    verify(exactly = 1) { libraryLifecycle.addLibrary(match { it.path == outsideLibrary }) }
  }

  private fun postLibrary(path: Path) =
    mockMvc.post("/api/v1/libraries") {
      contentType = MediaType.APPLICATION_JSON
      content = objectMapper.writeValueAsString(LibraryCreationDto(name = path.fileName.toString(), root = path.toString()))
    }
}
