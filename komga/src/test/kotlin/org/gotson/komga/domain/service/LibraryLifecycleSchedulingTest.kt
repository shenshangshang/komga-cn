package org.gotson.komga.domain.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.application.scheduler.LibraryScanScheduler
import org.gotson.komga.application.tasks.TaskEmitter
import org.gotson.komga.domain.model.Library
import org.gotson.komga.domain.persistence.LibraryRepository
import org.gotson.komga.domain.persistence.SeriesRepository
import org.gotson.komga.domain.persistence.SidecarRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.support.TransactionTemplate
import java.nio.file.Path

class LibraryLifecycleSchedulingTest {
  private val libraryRepository = mockk<LibraryRepository>(relaxed = true)
  private val seriesLifecycle = mockk<SeriesLifecycle>(relaxed = true)
  private val seriesRepository = mockk<SeriesRepository>(relaxed = true)
  private val sidecarRepository = mockk<SidecarRepository>(relaxed = true)
  private val taskEmitter = mockk<TaskEmitter>(relaxed = true)
  private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
  private val transactionTemplate = mockk<TransactionTemplate>(relaxed = true)
  private val libraryScanScheduler = mockk<LibraryScanScheduler>(relaxed = true)
  private val lifecycle =
    LibraryLifecycle(
      libraryRepository = libraryRepository,
      seriesLifecycle = seriesLifecycle,
      seriesRepository = seriesRepository,
      sidecarRepository = sidecarRepository,
      taskEmitter = taskEmitter,
      eventPublisher = eventPublisher,
      transactionTemplate = transactionTemplate,
      libraryScanScheduler = libraryScanScheduler,
    )

  @Test
  fun `adding a library schedules its periodic scan`(
    @TempDir root: Path,
  ) {
    val library = Library("library", root.toUri().toURL())
    every { libraryRepository.findAll() } returns emptyList()
    every { libraryRepository.findById(library.id) } returns library

    val created = lifecycle.addLibrary(library)

    assertThat(created).isEqualTo(library)
    verify(exactly = 1) { libraryScanScheduler.scheduleScan(library) }
  }

  @Test
  fun `deleting a library unschedules its periodic scan`(
    @TempDir root: Path,
  ) {
    val library = Library("library", root.toUri().toURL())
    every { seriesRepository.findAllByLibraryId(library.id) } returns emptyList()

    lifecycle.deleteLibrary(library)

    verify(exactly = 1) { libraryScanScheduler.unscheduleScan(library.id) }
  }
}
