package org.gotson.komga.infrastructure.datasource

/**
 * Kept for compatibility with the original SQLite implementation.
 * Holds MySQL collation names used across jOOQ queries.
 */
object SqliteUdfDataSource {
  // MySQL is accent-insensitive with utf8mb4_0900_ai_ci, no UDF needed
  const val UDF_STRIP_ACCENTS = "UDF_STRIP_ACCENTS"

  // unicode-aware collation, equivalent to ICU tertiary strength collator
  const val COLLATION_UNICODE_3 = "utf8mb4_unicode_ci"
}
