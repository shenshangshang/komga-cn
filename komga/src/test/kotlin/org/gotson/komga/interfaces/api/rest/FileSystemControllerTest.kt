package org.gotson.komga.interfaces.api.rest

import com.fasterxml.jackson.databind.ObjectMapper
import org.gotson.komga.infrastructure.configuration.KomgaSettingsProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.nio.file.Files
import java.nio.file.Path

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class FileSystemControllerTest(
  @Autowired private val mockMvc: MockMvc,
  @Autowired private val settings: KomgaSettingsProvider,
  @Autowired private val objectMapper: ObjectMapper,
) {
  private val route = "/api/v1/filesystem"

  @AfterEach
  fun cleanup() {
    settings.libraryCreationAllowedRoots = emptyList()
  }

  @Test
  @WithAnonymousUser
  fun `given anonymous user when getDirectoryListing then return unauthorized`() {
    mockMvc
      .post(route)
      .andExpect { status { isUnauthorized() } }
  }

  @Test
  @WithMockUser
  fun `given regular user when getDirectoryListing then return forbidden`() {
    mockMvc
      .post(route)
      .andExpect { status { isForbidden() } }
  }

  @Test
  @WithMockCustomUser(roles = ["CREATE_LIBRARY"])
  fun `given library creator when listing roots then only configured roots are returned`(
    @TempDir parent: Path,
  ) {
    val allowed = Files.createDirectory(parent.resolve("allowed"))
    Files.createDirectory(parent.resolve("private"))
    settings.libraryCreationAllowedRoots = listOf(allowed.toString())

    mockMvc
      .post(route) {
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsString(DirectoryRequestDto())
      }.andExpect {
        status { isOk() }
        jsonPath("directories.length()") { value(1) }
        jsonPath("directories[0].path") { value(allowed.toRealPath().toString()) }
        jsonPath("files.length()") { value(0) }
        jsonPath("parent") { doesNotExist() }
      }
  }

  @Test
  @WithMockCustomUser(roles = ["CREATE_LIBRARY"])
  fun `given library creator when browsing configured root then cannot navigate above it`(
    @TempDir parent: Path,
  ) {
    val allowed = Files.createDirectory(parent.resolve("allowed"))
    val child = Files.createDirectory(allowed.resolve("child"))
    Files.writeString(allowed.resolve("private.cbz"), "private")
    settings.libraryCreationAllowedRoots = listOf(allowed.toString())

    mockMvc
      .post(route) {
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsString(DirectoryRequestDto(allowed.toString(), showFiles = true))
      }.andExpect {
        status { isOk() }
        jsonPath("directories.length()") { value(1) }
        jsonPath("directories[0].path") { value(child.toRealPath().toString()) }
        jsonPath("files.length()") { value(0) }
        jsonPath("parent") { doesNotExist() }
      }

    mockMvc
      .post(route) {
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsString(DirectoryRequestDto(parent.toString()))
      }.andExpect { status { isForbidden() } }
  }

  @Test
  @WithMockUser(roles = ["ADMIN"])
  fun `given relative path param when getDirectoryListing then return bad request`() {
    mockMvc
      .post(route) {
        contentType = MediaType.APPLICATION_JSON
        content = "."
      }.andExpect { status { isBadRequest() } }
  }

  @Test
  @WithMockUser(roles = ["ADMIN"])
  fun `given non-existent path param when getDirectoryListing then return bad request`(
    @TempDir parent: Path,
  ) {
    Files.delete(parent)

    mockMvc
      .post(route) {
        contentType = MediaType.APPLICATION_JSON
        content = parent.toString()
      }.andExpect { status { isBadRequest() } }
  }
}
