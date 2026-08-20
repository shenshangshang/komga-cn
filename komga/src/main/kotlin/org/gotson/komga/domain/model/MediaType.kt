package org.gotson.komga.domain.model

enum class MediaType(
  val type: String,
  val profile: MediaProfile,
  val fileExtension: String,
  val exportType: String = type,
) {
  ZIP("application/zip", MediaProfile.DIVINA, "cbz", "application/vnd.comicbook+zip"),
  DIRECTORY("application/vnd.komga.image-directory", MediaProfile.DIVINA, "cbz", "application/vnd.comicbook+zip"),
  RAR_GENERIC("application/x-rar-compressed", MediaProfile.DIVINA, "cbr", "application/vnd.comicbook-rar"),
  RAR_4("application/x-rar-compressed; version=4", MediaProfile.DIVINA, "cbr", "application/vnd.comicbook-rar"),
  RAR_5("application/x-rar-compressed; version=5", MediaProfile.DIVINA, "cbr", "application/vnd.comicbook-rar"),
  EPUB("application/epub+zip", MediaProfile.EPUB, "epub"),
  PDF("application/pdf", MediaProfile.PDF, "pdf"),
  MOBI("application/x-mobipocket-ebook", MediaProfile.MOBI, "mobi"),
  // Video formats — Tika may report variant MIME strings (e.g. video/x-matroska for mkv),
  // so fromMediaType also falls back to prefix matching for the VIDEO/AUDIO profiles.
  MP4("video/mp4", MediaProfile.VIDEO, "mp4"),
  MKV("video/x-matroska", MediaProfile.VIDEO, "mkv"),
  WEBM("video/webm", MediaProfile.VIDEO, "webm"),
  MOV("video/quicktime", MediaProfile.VIDEO, "mov"),
  AVI("video/x-msvideo", MediaProfile.VIDEO, "avi"),
  // Audio formats
  MP3("audio/mpeg", MediaProfile.AUDIO, "mp3"),
  FLAC("audio/flac", MediaProfile.AUDIO, "flac"),
  M4A("audio/mp4", MediaProfile.AUDIO, "m4a"),
  OGG("audio/ogg", MediaProfile.AUDIO, "ogg"),
  WAV("audio/vnd.wave", MediaProfile.AUDIO, "wav"),
  ;

  companion object {
    fun fromMediaType(mediaType: String?): MediaType? {
      if (mediaType == null) return null
      // exact match first
      entries.firstOrNull { it.type == mediaType }?.let { return it }
      // Tika returns variant MIME strings for some video/audio containers
      // (e.g. audio/x-flac, audio/x-wav, video/x-matroska-3d). Fall back to
      // prefix matching so these are still recognized.
      return when {
        mediaType.startsWith("video/") -> matchingMediaProfile(MediaProfile.VIDEO).firstOrNull()
        mediaType.startsWith("audio/") -> matchingMediaProfile(MediaProfile.AUDIO).firstOrNull()
        else -> null
      }
    }

    fun matchingMediaProfile(mediaProfile: MediaProfile): Collection<MediaType> = entries.filter { it.profile == mediaProfile }
  }
}
