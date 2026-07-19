package org.gotson.komga.application.tasks

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.gotson.komga.domain.model.makeBook
import org.gotson.komga.domain.model.makeLibrary
import org.gotson.komga.domain.model.makeSeries
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
  fun `deleting a regular book removes files and metadata then cleans the library`() {
    val library = makeLibrary(id = "library")
    val series = makeSeries(name = "series", libraryId = library.id)
    val book = makeBook("book", id = "book", seriesId = series.id, libraryId = library.id)
    every { bookRepository.findByIdOrNull(book.id) } returns book
    every { seriesRepository.findByIdOrNull(series.id) } returns series
    every { libraryRepository.findByIdOrNull(library.id) } returns library
    every { bookRepository.findAllBySeriesId(series.id) } returns emptyList()

    handler.handleTask(Task.DeleteBook(book.id))

    verifyOrder {
      bookLifecycle.deleteBookFiles(book)
      bookLifecycle.deleteOne(book)
      seriesLifecycle.deleteMany(listOf(series))
      libraryContentLifecycle.emptyTrash(library)
    }
  }

  @Test
  fun `deleting one book keeps and reorders a non-empty series`() {
    val library = makeLibrary(id = "library")
    val series = makeSeries(name = "series", libraryId = library.id)
    val book = makeBook("book", id = "book", seriesId = series.id, libraryId = library.id)
    val remaining = makeBook("remaining", id = "remaining", seriesId = series.id, libraryId = library.id)
    every { bookRepository.findByIdOrNull(book.id) } returns book
    every { seriesRepository.findByIdOrNull(series.id) } returns series
    every { libraryRepository.findByIdOrNull(library.id) } returns library
    every { bookRepository.findAllBySeriesId(series.id) } returns listOf(remaining)

    handler.handleTask(Task.DeleteBook(book.id))

    verifyOrder {
      bookLifecycle.deleteBookFiles(book)
      bookLifecycle.deleteOne(book)
      seriesLifecycle.sortBooks(series)
      libraryContentLifecycle.emptyTrash(library)
    }
    verify(exactly = 0) { seriesLifecycle.deleteMany(any()) }
  }

  @Test
  fun `deleting a series immediately empties its database trash`() {
    val library = makeLibrary(id = "library")
    val series = makeSeries(name = "series", libraryId = library.id)
    every { seriesRepository.findByIdOrNull(series.id) } returns series
    every { libraryRepository.findByIdOrNull(library.id) } returns library

    handler.handleTask(Task.DeleteSeries(series.id))

    verifyOrder {
      seriesLifecycle.deleteSeriesFiles(series)
      libraryContentLifecycle.emptyTrash(library)
    }
  }
}
