package org.gotson.komga.application.scheduler

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.application.tasks.TaskEmitter
import org.gotson.komga.domain.model.Library
import org.gotson.komga.domain.model.makeLibrary
import org.junit.jupiter.api.Test
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.time.Duration

class LibraryScanSchedulerTest {
  @Test
  fun `every 15 minutes converts to the expected duration`() {
    assertThat(Library.ScanInterval.EVERY_15M.toDuration()).isEqualTo(Duration.ofMinutes(15))
  }

  @Test
  fun `unscheduling a library cancels and removes its periodic scan`() {
    val taskScheduler = ThreadPoolTaskScheduler().apply { initialize() }
    val scheduler = LibraryScanScheduler(taskScheduler, mockk<TaskEmitter>(relaxed = true))
    val library = makeLibrary().copy(scanInterval = Library.ScanInterval.EVERY_15M)

    try {
      scheduler.scheduleScan(library)
      assertThat(scheduler.scheduledTasks).hasSize(1)

      scheduler.unscheduleScan(library.id)
      assertThat(scheduler.scheduledTasks).isEmpty()
    } finally {
      taskScheduler.shutdown()
    }
  }
}
