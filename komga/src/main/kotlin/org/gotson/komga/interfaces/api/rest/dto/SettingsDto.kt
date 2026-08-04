package org.gotson.komga.interfaces.api.rest.dto

import org.gotson.komga.infrastructure.configuration.RegistrationMode

data class SettingsDto(
  val registrationMode: RegistrationMode,
  val siteUrl: String?,
  val libraryCreationAllowedRoots: List<String>,
  val deleteEmptyCollections: Boolean,
  val deleteEmptyReadLists: Boolean,
  val rememberMeDurationDays: Long,
  val thumbnailSize: ThumbnailSizeDto,
  val taskPoolSize: Int,
  val serverPort: SettingMultiSource<Int?>,
  val serverContextPath: SettingMultiSource<String?>,
  val koboProxy: Boolean,
  val koboPort: Int?,
  val kepubifyPath: SettingMultiSource<String?>,
  val prefetchPages: Int,
)

data class SettingMultiSource<T>(
  val configurationSource: T,
  val databaseSource: T,
  val effectiveValue: T,
)
