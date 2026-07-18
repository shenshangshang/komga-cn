package org.gotson.komga.infrastructure.jooq

import com.github.f4b6a3.tsid.TsidCreator
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.io.Closeable

/**
 * Temporary table with a single STRING column.
 * This is made to store collection of values that are too long to be specified in a query condition,
 * by using a sub-select instead.
 *
 * The table name is automatically generated, and the table is dropped when the object is closed.
 */
class TempTable(
  dslContext: DSLContext,
) : Closeable {
  private val connectionProvider = dslContext.configuration().connectionProvider()
  private val connection = connectionProvider.acquire()

  /**
   * A DSL context pinned to the physical connection that owns this temporary table.
   * Queries referencing [selectTempStrings] must execute through this context.
   */
  val dsl: DSLContext = DSL.using(dslContext.configuration().derive(connection))

  val name: String = generateName()

  private var created = false
  private var closed = false

  fun create() {
    check(!closed) { "Temporary table is already closed" }
    dsl.execute("CREATE TEMPORARY TABLE $name (STRING varchar(768) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL);")
    created = true
  }

  fun insertTempStrings(
    batchSize: Int,
    collection: Collection<String>,
  ) {
    if (!created) create()
    if (collection.isNotEmpty()) {
      collection.chunked(batchSize).forEach { chunk ->
        dsl
          .batch(
            dsl.insertInto(DSL.table(DSL.name(name)), DSL.field(DSL.name("STRING"), String::class.java)).values(null as String?),
          ).also { step ->
            chunk.forEach {
              step.bind(it)
            }
          }.execute()
      }
    }
  }

  fun selectTempStrings() = dsl.select(DSL.field(DSL.name("STRING"), String::class.java)).from(DSL.table(DSL.name(name)))

  override fun close() {
    if (closed) return
    closed = true

    try {
      if (created) dsl.dropTableIfExists(name).execute()
    } finally {
      connectionProvider.release(connection)
    }
  }

  companion object {
    private fun generateName() = "temp_${TsidCreator.getTsid256()}"

    fun DSLContext.withTempTable(
      batchSize: Int,
      collection: Collection<String>,
    ): TempTable {
      val tempTable = TempTable(this)
      try {
        tempTable.insertTempStrings(batchSize, collection)
        return tempTable
      } catch (throwable: Throwable) {
        try {
          tempTable.close()
        } catch (closeException: Throwable) {
          throwable.addSuppressed(closeException)
        }
        throw throwable
      }
    }
  }
}
