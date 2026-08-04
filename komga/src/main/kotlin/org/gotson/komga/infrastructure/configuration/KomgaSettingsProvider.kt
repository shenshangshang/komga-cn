package org.gotson.komga.infrastructure.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.RandomStringUtils
import org.gotson.komga.domain.model.ThumbnailSize
import org.gotson.komga.infrastructure.jooq.main.ServerSettingsDao
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@Service
class KomgaSettingsProvider(
  private val serverSettingsDao: ServerSettingsDao,
  private val eventPublisher: ApplicationEventPublisher,
  private val objectMapper: ObjectMapper,
) {
  var registrationMode: RegistrationMode =
    serverSettingsDao
      .getSettingByKey(Settings.REGISTRATION_MODE.name, String::class.java)
      ?.let { runCatching { RegistrationMode.valueOf(it) }.getOrNull() }
      ?: RegistrationMode.DISABLED
    set(value) {
      serverSettingsDao.saveSetting(Settings.REGISTRATION_MODE.name, value.name)
      field = value
    }
  var siteUrl: String? =
    serverSettingsDao.getSettingByKey(Settings.SITE_URL.name, String::class.java)?.ifBlank { null }
    set(value) {
      val normalized = value?.trim()?.trimEnd('/')?.ifBlank { null }
      if (normalized != null)
        serverSettingsDao.saveSetting(Settings.SITE_URL.name, normalized)
      else
        serverSettingsDao.deleteSetting(Settings.SITE_URL.name)
      field = normalized
    }
  var libraryCreationAllowedRoots: List<String> =
    serverSettingsDao
      .getSettingByKey(Settings.LIBRARY_CREATION_ALLOWED_ROOTS.name, String::class.java)
      ?.let { encoded -> runCatching { objectMapper.readValue(encoded, Array<String>::class.java).toList() }.getOrNull() }
      ?: emptyList()
    set(value) {
      val normalized =
        value
          .map { rawPath ->
            val path = Paths.get(rawPath.trim())
            require(path.isAbsolute) { "Allowed library root must be an absolute path: $rawPath" }
            val realPath =
              runCatching { path.toRealPath() }
                .getOrElse { throw IllegalArgumentException("Allowed library root does not exist: $rawPath") }
            require(Files.isDirectory(realPath)) { "Allowed library root is not a directory: $rawPath" }
            realPath.toString()
          }.distinct()
          .sortedWith(String.CASE_INSENSITIVE_ORDER)
      if (normalized.isNotEmpty())
        serverSettingsDao.saveSetting(Settings.LIBRARY_CREATION_ALLOWED_ROOTS.name, objectMapper.writeValueAsString(normalized))
      else
        serverSettingsDao.deleteSetting(Settings.LIBRARY_CREATION_ALLOWED_ROOTS.name)
      field = normalized
    }
  var deleteEmptyCollections: Boolean =
    serverSettingsDao.getSettingByKey(Settings.DELETE_EMPTY_COLLECTIONS.name, Boolean::class.java) ?: false
    set(value) {
      serverSettingsDao.saveSetting(Settings.DELETE_EMPTY_COLLECTIONS.name, value)
      field = value
    }

  var deleteEmptyReadLists: Boolean =
    serverSettingsDao.getSettingByKey(Settings.DELETE_EMPTY_READLISTS.name, Boolean::class.java) ?: false
    set(value) {
      serverSettingsDao.saveSetting(Settings.DELETE_EMPTY_READLISTS.name, value)
      field = value
    }

  var rememberMeKey: String =
    serverSettingsDao.getSettingByKey(Settings.REMEMBER_ME_KEY.name, String::class.java)
      ?: getRandomRememberMeKey().also { rememberMeKey = it }
    set(value) {
      serverSettingsDao.saveSetting(Settings.REMEMBER_ME_KEY.name, value)
      field = value
    }

  fun renewRememberMeKey() {
    rememberMeKey = getRandomRememberMeKey()
  }

  private fun getRandomRememberMeKey() = RandomStringUtils.secure().nextAlphanumeric(32)

  var rememberMeDuration: Duration =
    (serverSettingsDao.getSettingByKey(Settings.REMEMBER_ME_DURATION.name, Int::class.java) ?: 365).days
    set(value) {
      serverSettingsDao.saveSetting(Settings.REMEMBER_ME_DURATION.name, value.inWholeDays.toInt())
      field = value
    }

  var thumbnailSize: ThumbnailSize =
    serverSettingsDao.getSettingByKey(Settings.THUMBNAIL_SIZE.name, String::class.java)?.let {
      ThumbnailSize.valueOf(it)
    } ?: ThumbnailSize.DEFAULT
    set(value) {
      serverSettingsDao.saveSetting(Settings.THUMBNAIL_SIZE.name, value.name)
      field = value
    }

  var taskPoolSize: Int =
    serverSettingsDao.getSettingByKey(Settings.TASK_POOL_SIZE.name, Int::class.java) ?: 1
    set(value) {
      serverSettingsDao.saveSetting(Settings.TASK_POOL_SIZE.name, value)
      field = value
      eventPublisher.publishEvent(SettingChangedEvent.TaskPoolSize)
    }

  var serverPort: Int? =
    serverSettingsDao.getSettingByKey(Settings.SERVER_PORT.name, Int::class.java)
    set(value) {
      if (value != null)
        serverSettingsDao.saveSetting(Settings.SERVER_PORT.name, value)
      else
        serverSettingsDao.deleteSetting(Settings.SERVER_PORT.name)
      field = value
    }

  var serverContextPath: String? =
    serverSettingsDao.getSettingByKey(Settings.SERVER_CONTEXT_PATH.name, String::class.java)
    set(value) {
      if (value != null)
        serverSettingsDao.saveSetting(Settings.SERVER_CONTEXT_PATH.name, value)
      else
        serverSettingsDao.deleteSetting(Settings.SERVER_CONTEXT_PATH.name)
      field = value
    }

  var koboProxy: Boolean =
    serverSettingsDao.getSettingByKey(Settings.KOBO_PROXY.name, Boolean::class.java) ?: false
    set(value) {
      serverSettingsDao.saveSetting(Settings.KOBO_PROXY.name, value)
      field = value
    }

  var koboPort: Int? =
    serverSettingsDao.getSettingByKey(Settings.KOBO_PORT.name, Int::class.java)
    set(value) {
      if (value != null)
        serverSettingsDao.saveSetting(Settings.KOBO_PORT.name, value)
      else
        serverSettingsDao.deleteSetting(Settings.KOBO_PORT.name)
      field = value
    }

  var kepubifyPath: String? =
    serverSettingsDao.getSettingByKey(Settings.KEPUBIFY_PATH.name, String::class.java)?.ifBlank { null }
    set(value) {
      if (value != null)
        serverSettingsDao.saveSetting(Settings.KEPUBIFY_PATH.name, value)
      else
        serverSettingsDao.deleteSetting(Settings.KEPUBIFY_PATH.name)
      field = value
      eventPublisher.publishEvent(SettingChangedEvent.KepubifyPath)
    }

  var prefetchPages: Int =
    serverSettingsDao.getSettingByKey(Settings.PREFETCH_PAGES.name, Int::class.java) ?: 3
    set(value) {
      serverSettingsDao.saveSetting(Settings.PREFETCH_PAGES.name, value)
      field = value
    }
}

private enum class Settings {
  REGISTRATION_MODE,
  SITE_URL,
  LIBRARY_CREATION_ALLOWED_ROOTS,
  DELETE_EMPTY_COLLECTIONS,
  DELETE_EMPTY_READLISTS,
  REMEMBER_ME_KEY,
  REMEMBER_ME_DURATION,
  THUMBNAIL_SIZE,
  TASK_POOL_SIZE,
  SERVER_PORT,
  SERVER_CONTEXT_PATH,
  KOBO_PROXY,
  KOBO_PORT,
  KEPUBIFY_PATH,
  PREFETCH_PAGES,
}

enum class RegistrationMode {
  DISABLED,
  OPEN,
  INVITE,
}
