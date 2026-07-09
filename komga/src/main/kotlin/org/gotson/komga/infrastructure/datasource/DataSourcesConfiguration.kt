package org.gotson.komga.infrastructure.datasource

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.gotson.komga.infrastructure.configuration.KomgaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@Configuration
class DataSourcesConfiguration(
  private val komgaProperties: KomgaProperties,
) {
  @Bean("mysqlDataSourceRW")
  @Primary
  fun mysqlDataSourceRW(): DataSource = buildDataSource("MysqlMainPool", komgaProperties.database)

  @Bean("mysqlDataSourceRO")
  fun mysqlDataSourceRO(): DataSource = mysqlDataSourceRW()

  @Bean("tasksDataSourceRW")
  fun tasksDataSourceRW(): DataSource = buildDataSource("MysqlTasksPool", komgaProperties.tasksDb)

  @Bean("tasksDataSourceRO")
  fun tasksDataSourceRO(): DataSource = tasksDataSourceRW()

  private fun buildDataSource(
    poolName: String,
    databaseProps: KomgaProperties.Database,
  ): HikariDataSource {
    val poolSize =
      databaseProps.poolSize
        ?: Runtime.getRuntime().availableProcessors().coerceAtMost(databaseProps.maxPoolSize)

    return HikariDataSource(
      HikariConfig().apply {
        jdbcUrl = databaseProps.url
        username = databaseProps.username
        password = databaseProps.password
        driverClassName = "com.mysql.cj.jdbc.Driver"
        this.poolName = poolName
        this.maximumPoolSize = poolSize
      },
    )
  }
}
