package org.gotson.komga.infrastructure.datasource

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.Ordered
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import java.util.UUID

internal data class MySqlTestSchemas(
  val main: String,
  val tasks: String,
)

internal object MySqlTestSchemaUrls {
  fun allocate(
    mainUrl: String,
    tasksUrl: String,
  ): MySqlTestSchemas {
    val suffix = UUID.randomUUID().toString().replace("-", "")
    return MySqlTestSchemas(
      withSchema(mainUrl, "komga_test_$suffix"),
      withSchema(tasksUrl, "komga_tasks_test_$suffix"),
    )
  }

  fun withSchema(
    url: String,
    schema: String,
  ): String {
    require(url.startsWith("jdbc:mysql://")) { "MySQL test isolation requires a jdbc:mysql URL" }
    val queryIndex = url.indexOf('?')
    val base = if (queryIndex < 0) url else url.substring(0, queryIndex)
    val query = if (queryIndex < 0) "" else url.substring(queryIndex + 1)
    val authorityEnd = base.indexOf('/', "jdbc:mysql://".length)
    require(authorityEnd >= 0) { "MySQL URL must include a schema path" }
    val options = listOf(query, "createDatabaseIfNotExist=true").filter { it.isNotEmpty() }.joinToString("&")
    return "${base.substring(0, authorityEnd + 1)}$schema?$options"
  }
}

class MySqlTestSchemaEnvironmentPostProcessor :
  EnvironmentPostProcessor,
  Ordered {
  override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE

  override fun postProcessEnvironment(
    environment: ConfigurableEnvironment,
    application: SpringApplication,
  ) {
    if (!environment.activeProfiles.contains("test")) return
    val main = environment.getProperty("komga.database.url") ?: return
    val tasks = environment.getProperty("komga.tasks-db.url") ?: return
    if (!main.startsWith("jdbc:mysql://") || !tasks.startsWith("jdbc:mysql://")) return
    val schemas = MySqlTestSchemaUrls.allocate(main, tasks)
    environment.propertySources.addFirst(
      MapPropertySource(
        "mysqlTestSchemaIsolation",
        mapOf("komga.database.url" to schemas.main, "komga.tasks-db.url" to schemas.tasks),
      ),
    )
  }
}
