package org.gotson.komga.infrastructure.datasource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import javax.sql.DataSource

@SpringBootTest
class DataSourcesConfigurationTest(
  @Autowired @Qualifier("mysqlDataSourceRW") private val dataSourceRW: DataSource,
  @Autowired @Qualifier("mysqlDataSourceRO") private val dataSourceRO: DataSource,
  @Autowired @Qualifier("tasksDataSourceRW") private val tasksDataSourceRW: DataSource,
  @Autowired @Qualifier("tasksDataSourceRO") private val tasksDataSourceRO: DataSource,
) {
  @Test
  fun `read and write aliases use the same MySQL pools`() {
    assertThat(dataSourceRW).isSameAs(dataSourceRO)
    assertThat(tasksDataSourceRW).isSameAs(tasksDataSourceRO)
    assertThat(dataSourceRW).isNotSameAs(tasksDataSourceRW)
  }

  @Test
  fun `tasks migrations use an independent Flyway history table`() {
    tasksDataSourceRW.connection.use { connection ->
      connection.prepareStatement(
        """
        SELECT COUNT(*)
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = ?
        """.trimIndent(),
      ).use { statement ->
        statement.setString(1, TASKS_FLYWAY_HISTORY_TABLE)
        statement.executeQuery().use { result ->
          assertThat(result.next()).isTrue()
          assertThat(result.getInt(1)).isEqualTo(1)
        }
      }

      connection.prepareStatement(
        "SELECT COUNT(*) FROM $TASKS_FLYWAY_HISTORY_TABLE WHERE SUCCESS = TRUE AND VERSION IN ('1', '2')",
      ).use { statement ->
        statement.executeQuery().use { result ->
          assertThat(result.next()).isTrue()
          assertThat(result.getInt(1)).isEqualTo(2)
        }
      }
    }
  }

  @Test
  fun `tasks migrations are idempotent in a shared schema`() {
    FlywaySecondaryMigrationInitializer(tasksDataSourceRW).afterPropertiesSet()

    tasksDataSourceRW.connection.use { connection ->
      connection.prepareStatement(
        "SELECT COUNT(*) FROM $TASKS_FLYWAY_HISTORY_TABLE WHERE SUCCESS = TRUE AND VERSION IN ('1', '2')",
      ).use { statement ->
        statement.executeQuery().use { result ->
          assertThat(result.next()).isTrue()
          assertThat(result.getInt(1)).isEqualTo(2)
        }
      }
    }
  }
}
