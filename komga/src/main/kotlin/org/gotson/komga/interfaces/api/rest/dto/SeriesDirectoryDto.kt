package org.gotson.komga.interfaces.api.rest.dto

data class SeriesDirectoryListingDto(
  val currentPath: String,
  val breadcrumbs: List<SeriesDirectoryBreadcrumbDto>,
  val directories: List<SeriesDirectoryDto>,
)

data class SeriesDirectoryBreadcrumbDto(
  val name: String,
  val path: String,
)

data class SeriesDirectoryDto(
  val name: String,
  val path: String,
  val parentPath: String,
  val directBooksCount: Int,
  val descendantBooksCount: Int,
  val childDirectoryCount: Int,
  val thumbnailBookId: String?,
)
