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
  }
}
