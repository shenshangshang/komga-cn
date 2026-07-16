package org.gotson.komga.infrastructure.datasource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MySqlTestSchemaUrlsTest {
  @Test
  fun `replace schema preserves connection options`() {
    assertThat(MySqlTestSchemaUrls.withSchema("jdbc:mysql://localhost:3306/komga?useSSL=false", "test_main"))
      .isEqualTo("jdbc:mysql://localhost:3306/test_main?useSSL=false&createDatabaseIfNotExist=true")
  }

  @Test
  fun `each allocation isolates both databases`() {
    val first = MySqlTestSchemaUrls.allocate("jdbc:mysql://localhost/komga", "jdbc:mysql://localhost/komga_tasks")
    val second = MySqlTestSchemaUrls.allocate("jdbc:mysql://localhost/komga", "jdbc:mysql://localhost/komga_tasks")

    assertThat(first).isNotEqualTo(second)
    assertThat(first.main).doesNotContain("/komga?")
    assertThat(first.tasks).doesNotContain("/komga_tasks?")
  }

  @Test
  fun `identical database urls allocate one shared schema`() {
    val schemas = MySqlTestSchemaUrls.allocate("jdbc:mysql://localhost/komga", "jdbc:mysql://localhost/komga")

    assertThat(schemas.main).isEqualTo(schemas.tasks)
    assertThat(schemas.main).contains("/komga_test_")
  }
}
