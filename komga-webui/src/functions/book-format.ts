import {BookFormat, MediaDto} from '@/types/komga-books'
import {lowerCase} from 'lodash'

export function getBookFormatFromMedia(media: MediaDto): BookFormat {
  switch (media.mediaType) {
    case 'application/x-rar-compressed':
    case 'application/x-rar-compressed; version=4':
      return {type: 'CBR', color: '#03A9F4'}
    case 'application/zip':
      return {type: 'CBZ', color: '#4CAF50'}
    case 'application/pdf':
      return {type: 'PDF', color: '#FF5722'}
    case 'application/epub+zip':
      return media.epubIsKepub ? {type: 'KEPUB', color: '#ff5ab1'} : {type: 'EPUB', color: '#ff5ab1'}
    case 'application/x-rar-compressed; version=5':
      return {type: 'RAR5', color: '#000000'}
    default:
      if (media.mediaType?.startsWith('video/')) return {type: media.mediaType.substring(6).toUpperCase(), color: '#9C27B0'}
      if (media.mediaType?.startsWith('audio/')) return {type: media.mediaType.substring(6).toUpperCase(), color: '#00BCD4'}
      return {type: media.mediaType, color: '#000000'}
  }
}

export function getBookFormatFromMediaType(mediaType: string): BookFormat {
  switch (mediaType) {
    case 'application/x-rar-compressed':
    case 'application/x-rar-compressed; version=4':
      return {type: 'CBR', color: '#03A9F4'}
    case 'application/zip':
      return {type: 'CBZ', color: '#4CAF50'}
    case 'application/pdf':
      return {type: 'PDF', color: '#FF5722'}
    case 'application/epub+zip':
      return {type: 'EPUB', color: '#ff5ab1'}
    case 'application/x-rar-compressed; version=5':
      return {type: 'RAR5', color: '#000000'}
    default:
      return {type: mediaType, color: '#000000'}
  }
}

export function getBookReadRouteFromMedia(media: MediaDto): string {
  switch (lowerCase(media.mediaProfile)) {
    case 'epub':
      return media.epubDivinaCompatible ? 'read-book' : 'read-epub'
    case 'video':
      return 'read-video'
    case 'audio':
      return 'read-audio'
    default:
      return 'read-book'
  }
}
