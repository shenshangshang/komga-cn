package org.gotson.komga.application.tasks

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.mockk
import io.mockk.verify
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.LibraryRepository
import org.gotson.komga.domain.persistence.SeriesRepository
import org.gotson.komga.domain.service.BookConverter
import org.gotson.komga.domain.service.BookImporter
import org.gotson.komga.domain.service.BookLifecycle
import org.gotson.komga.domain.service.BookMetadataLifecycle
import org.gotson.komga.domain.service.BookPageEditor
import org.gotson.komga.domain.service.LibraryContentLifecycle
import org.gotson.komga.domain.service.LocalArtworkLifecycle
import org.gotson.komga.domain.service.PageHashLifecycle
import org.gotson.komga.domain.service.SeriesLifecycle
import org.gotson.komga.domain.service.SeriesMetadataLifecycle
import org.gotson.komga.infrastructure.search.SearchIndexLifecycle
import org.junit.jupiter.api.Test

class TaskHandlerDeleteTest {
  private val taskEmitter = mockk<TaskEmitter>(relaxed = true)
  private val libraryRepository = mockk<LibraryRepository>(relaxed = true)
  private val bookRepository = mockk<BookRepository>(relaxed = true)
  private val seriesRepository = mockk<SeriesRepository>(relaxed = true)
  private val libraryContentLifecycle = mockk<LibraryContentLifecycle>(relaxed = true)
  private val bookLifecycle = mockk<BookLifecycle>(relaxed = true)
  private val bookMetadataLifecycle = mockk<BookMetadataLifecycle>(relaxed = true)
  private val seriesLifecycle = mockk<SeriesLifecycle>(relaxed = true)
  private val seriesMetadataLifecycle = mockk<SeriesMetadataLifecycle>(relaxed = true)
  private val localArtworkLifecycle = mockk<LocalArtworkLifecycle>(relaxed = true)
  private val bookImporter = mockk<BookImporter>(relaxed = true)
  private val bookConverter = mockk<BookConverter>(relaxed = true)
  private val bookPageEditor = mockk<BookPageEditor>(relaxed = true)
  private val searchIndexLifecycle = mockk<SearchIndexLifecycle>(relaxed = true)
  private val pageHashLifecycle = mockk<PageHashLifecycle>(relaxed = true)

  private val handler =
    TaskHandler(
      taskEmitter,
      libraryRepository,
      bookRepository,
      seriesRepository,
      libraryContentLifecycle,
      bookLifecycle,
      bookMetadataLifecycle,
      seriesLifecycle,
      seriesMetadataLifecycle,
      localArtworkLifecycle,
      bookImporter,
      bookConverter,
      bookPageEditor,
      searchIndexLifecycle,
      pageHashLifecycle,
      SimpleMeterRegistry(),
    )

  @Test
  fun `delete book tasks delegate to the scan-locked content lifecycle`() {
    handler.handleTask(Task.DeleteBook("book"))

    verify(exactly = 1) { libraryContentLifecycle.deleteBook("book") }
  }

  @Test
  fun `delete series tasks delegate to the scan-locked content lifecycle`() {
    handler.handleTask(Task.DeleteSeries("series"))

    verify(exactly = 1) { libraryContentLifecycle.deleteSeries("series") }
  }
}
