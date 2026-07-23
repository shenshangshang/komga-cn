package org.gotson.komga.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URL

class HistoricalEventTest {
  @Test
  fun `book events retain a readable book name`() {
    val book = makeBook(name = "小飞鼠单话", url = URL("file:/data/Comic/%E5%B0%8F%E9%A3%9E%E9%BC%A0/%E5%8D%95%E8%AF%9D"))
    val series = makeSeries(name = "小飞鼠合集")

    val deleted = HistoricalEvent.BookFileDeleted(book, "deleted")
    val imported = HistoricalEvent.BookImported(book, series, book.path, false)
    val analyzed = HistoricalEvent.BookAnalyzed(book)

    assertThat(deleted.properties["bookName"]).isEqualTo("小飞鼠单话")
    assertThat(imported.properties["bookName"]).isEqualTo("小飞鼠单话")
    assertThat(imported.properties["seriesName"]).isEqualTo("小飞鼠合集")
    assertThat(analyzed.properties["bookName"]).isEqualTo("小飞鼠单话")
  }
}
