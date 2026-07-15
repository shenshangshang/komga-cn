package org.gotson.komga.infrastructure.jooq

import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.infrastructure.jooq.TempTable.Companion.withTempTable
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest(properties = ["komga.database.pool-size=8"])
class TempTableTest(
  @Autowired @Qualifier("dslContextRW") private val dslContext: DSLContext,
) {
  @Test
  fun `temporary table lifecycle stays on one physical connection under concurrency`() {
    val executor = Executors.newFixedThreadPool(16)

    try {
      val futures =
        (1..64).map { index ->
          executor.submit<List<String>> {
            val expected = listOf("value-$index-a", "value-$index-b")

            dslContext.withTempTable(1, expected).use { tempTable ->
              val stringField = DSL.field(DSL.name("STRING"), String::class.java)

              tempTable.selectTempStrings().fetch(stringField)
            }
          }
        }

      futures.forEachIndexed { index, future ->
        assertThat(future.get(30, TimeUnit.SECONDS))
          .containsExactlyInAnyOrder("value-${index + 1}-a", "value-${index + 1}-b")
      }
    } finally {
      executor.shutdownNow()
    }
  }
}
