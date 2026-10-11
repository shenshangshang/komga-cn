package org.gotson.komga.infrastructure.datasource

import org.junit.platform.launcher.LauncherSession
import org.junit.platform.launcher.LauncherSessionListener
import org.testcontainers.containers.MySQLContainer

/**
 * Boots a throwaway MySQL for the test run when no database URL is provided.
 *
 * The integration tests need a real MySQL (see [MySqlTestSchemaEnvironmentPostProcessor], which
 * allocates an isolated `komga_test_<uuid>` schema per run). Previously that required the operator
 * to start MySQL by hand and export `KOMGA_DATABASE_URL`, so `./gradlew test` failed out of the box.
 *
 * Resolution order (first match wins):
 *  1. an explicit `-Dkomga.database.url` system property,
 *  2. a `KOMGA_DATABASE_URL` environment variable,
 *  3. a Testcontainers-managed MySQL started here.
 *
 * When Docker is unavailable the container cannot start; we log and continue so that the pure
 * unit tests (which need no database) still run.
 */
class MySqlTestContainerLauncher : LauncherSessionListener {
  override fun launcherSessionOpened(session: LauncherSession) {
    if (System.getProperty("komga.database.url") != null) return
    if (System.getenv("KOMGA_DATABASE_URL") != null) return

    try {
      val container = MySQLContainer<Nothing>("mysql:8.4")
      container.withDatabaseName("komga")
      // root: the test schema post-processor creates isolated schemas with createDatabaseIfNotExist
      container.withUsername("root")
      container.withPassword("komga-test")
      container.start()
      Runtime.getRuntime().addShutdownHook(Thread { runCatching { container.stop() } })

      System.setProperty("komga.database.url", container.jdbcUrl)
      System.setProperty("komga.database.username", container.username)
      System.setProperty("komga.database.password", container.password)
      System.setProperty("komga.tasks-db.url", container.jdbcUrl)
      System.setProperty("komga.tasks-db.username", container.username)
      System.setProperty("komga.tasks-db.password", container.password)
      System.err.println("MySqlTestContainerLauncher: started MySQL test container at ${container.jdbcUrl}")
    } catch (e: Throwable) {
      System.err.println(
        "MySqlTestContainerLauncher: could not start a MySQL test container (${e.message}). " +
          "Database-backed tests will fail unless KOMGA_DATABASE_URL points to a MySQL 8.x server.",
      )
    }
  }
}
