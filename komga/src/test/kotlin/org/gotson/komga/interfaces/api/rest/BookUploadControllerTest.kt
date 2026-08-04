package org.gotson.komga.interfaces.api.rest

import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.domain.model.CopyMode
import org.gotson.komga.domain.model.makeBook
import org.gotson.komga.domain.model.makeLibrary
import org.gotson.komga.domain.model.makeSeries
import org.gotson.komga.domain.persistence.LibraryRepository
import org.gotson.komga.domain.persistence.SeriesRepository
import org.gotson.komga.domain.service.BookImporter
import org.gotson.komga.domain.service.SeriesLifecycle
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.multipart
import java.nio.file.Path
import kotlin.io.path.exists

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@ActiveProfiles("test")
class BookUploadControllerTest(
  @Autowired private val mockMvc: MockMvc,
  @Autowired private val libraryRepository: LibraryRepository,
  @Autowired private val seriesRepository: SeriesRepository,
  @Autowired private val seriesLifecycle: SeriesLifecycle,
) {
  @MockkBean
  private lateinit var bookImporter: BookImporter

  @TempDir
  private lateinit var libraryRoot: Path

  @BeforeEach
  fun setup() {
    libraryRepository.insert(makeLibrary(name = "Allowed", id = "allowed", url = libraryRoot.toUri().toURL()))
    every { bookImporter.importBook(any(), any(), any(), any(), any()) } returns makeBook("uploaded")
  }

  @AfterEach
  fun cleanup() {
    seriesLifecycle.deleteMany(seriesRepository.findAll())
    libraryRepository.deleteAll()
    clearMocks(bookImporter)
  }

  @Test
  @WithMockCustomUser(roles = ["UPLOAD_BOOK"], sharedAllLibraries = false, sharedLibraries = ["allowed"])
  fun `upload can target an existing series in the selected library`() {
    val series = seriesLifecycle.createSeries(makeSeries("Existing", libraryId = "allowed", url = libraryRoot.resolve("Existing").toUri().toURL()))

    mockMvc.multipart("/api/v1/books/upload") {
      file(archive())
      param("libraryId", "allowed")
      param("seriesId", series.id)
    }.andExpect { status { isCreated() } }

    verify(exactly = 1) {
      bookImporter.importBook(any(), match { it.id == series.id }, CopyMode.COPY, "chapter", null)
    }
  }

  @Test
  @WithMockCustomUser(roles = ["UPLOAD_BOOK"], sharedAllLibraries = false, sharedLibraries = ["allowed"])
  fun `upload can create a new series in the selected library`() {
    mockMvc.multipart("/api/v1/books/upload") {
      file(archive())
      param("libraryId", "allowed")
      param("seriesName", "新系列")
    }.andExpect { status { isCreated() } }

    val created = seriesRepository.findAll().single()
    assertThat(created.name).isEqualTo("新系列")
    assertThat(created.libraryId).isEqualTo("allowed")
    assertThat(libraryRoot.resolve("新系列").exists()).isTrue()
    verify(exactly = 1) {
      bookImporter.importBook(any(), match { it.id == created.id }, CopyMode.COPY, "chapter", null)
    }
  }

  @Test
  @WithMockCustomUser(roles = ["UPLOAD_BOOK"], sharedAllLibraries = false, sharedLibraries = ["allowed"])
  fun `upload rejects a new series path traversal`() {
    mockMvc.multipart("/api/v1/books/upload") {
      file(archive())
      param("libraryId", "allowed")
      param("seriesName", "../escaped")
    }.andExpect { status { isBadRequest() } }

    assertThat(libraryRoot.parent.resolve("escaped").exists()).isFalse()
    assertThat(seriesRepository.findAll()).isEmpty()
    verify(exactly = 0) { bookImporter.importBook(any(), any(), any(), any(), any()) }
  }

  @Test
  @WithMockCustomUser(roles = ["UPLOAD_BOOK"], sharedAllLibraries = false)
  fun `upload rejects a library the user cannot access`() {
    mockMvc.multipart("/api/v1/books/upload") {
      file(archive())
      param("libraryId", "allowed")
      param("seriesName", "Forbidden")
    }.andExpect { status { isForbidden() } }

    assertThat(libraryRoot.resolve("Forbidden").exists()).isFalse()
    verify(exactly = 0) { bookImporter.importBook(any(), any(), any(), any(), any()) }
  }

  private fun archive() = MockMultipartFile("file", "chapter.cbz", "application/vnd.comicbook+zip", byteArrayOf(1))
}
