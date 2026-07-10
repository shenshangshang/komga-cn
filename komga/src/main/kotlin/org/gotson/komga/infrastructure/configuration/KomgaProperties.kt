package org.gotson.komga.infrastructure.configuration

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.convert.DurationUnit
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.time.Duration
import java.time.temporal.ChronoUnit

@Component
@ConfigurationProperties(prefix = "komga")
@Validated
class KomgaProperties {
  var findDuplicatePages: Boolean = true

  @Positive
  var pageHashing: Int = 3

  @Positive
  var epubDivinaLetterCountThreshold: Int = 15

  var oauth2AccountCreation: Boolean = false

  var oidcEmailVerification: Boolean = true

  var database = Database()

  var tasksDb = Database()

  var cors = Cors()

  var lucene = Lucene()

  var configDir: String? = null

  var kobo = Kobo()

  val fonts = Fonts()

  var prefetch = Prefetch()

  class Cors {
    var allowedOrigins: List<String> = emptyList()
  }

  class Database {
    @get:NotBlank
    var url: String = ""

    var username: String = ""

    var password: String = ""

    @get:Positive
    var batchChunkSize: Int = 1000

    @get:Positive
    var poolSize: Int? = null

    @get:Positive
    var maxPoolSize: Int = 8

    @DurationUnit(ChronoUnit.SECONDS)
    var busyTimeout: Duration? = null

    var checkLocalFilesystem: Boolean = true
  }

  class Fonts {
    @get:NotBlank
    var dataDirectory: String = ""
  }

  class Lucene {
    @get:NotBlank
    var dataDirectory: String = ""

    var indexAnalyzer = IndexAnalyzer()

    @DurationUnit(ChronoUnit.SECONDS)
    var commitDelay: Duration = Duration.ofSeconds(2)

    class IndexAnalyzer {
      @get:Positive
      var minGram: Int = 3

      @get:Positive
      var maxGram: Int = 10

      var preserveOriginal: Boolean = true
    }
  }

  class Prefetch {
    @get:Min(0)
    @get:Max(10)
    var pages: Int = 3
  }

  class Kobo {
    @get:Positive
    var syncItemLimit: Int = 100

    var kepubifyPath: String? = null
  }
}
