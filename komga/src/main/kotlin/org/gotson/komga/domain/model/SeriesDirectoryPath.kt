package org.gotson.komga.domain.model

private val encodedPathSeparatorOrTraversal = Regex("%(?:2e|2f|5c)", RegexOption.IGNORE_CASE)
private val windowsDrivePath = Regex("^[A-Za-z]:")

/**
 * Validate the portable, `/` separated path stored relative to a series root.
 * The empty string denotes the series root itself.
 */
fun normalizeSeriesDirectoryPath(raw: String?): String {
  val path = raw.orEmpty()
  require(path == path.trim()) { "Directory path cannot contain surrounding whitespace" }
  require(!path.startsWith('/') && !path.endsWith('/')) { "Directory path must be relative" }
  require(!windowsDrivePath.containsMatchIn(path)) { "Directory path must not use a drive prefix" }
  require(!path.contains('\\')) { "Directory path must use forward slashes" }
  require(!encodedPathSeparatorOrTraversal.containsMatchIn(path)) { "Encoded directory traversal is not allowed" }
  require(path.isEmpty() || path.split('/').none { it.isEmpty() || it == "." || it == ".." }) { "Directory path contains an invalid segment" }
  return path
}
