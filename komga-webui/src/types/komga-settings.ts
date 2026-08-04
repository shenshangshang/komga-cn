export interface SettingsDto {
  registrationMode: RegistrationMode,
  deleteEmptyCollections: boolean,
  deleteEmptyReadLists: boolean,
  rememberMeDurationDays: number,
  thumbnailSize: ThumbnailSizeDto,
  taskPoolSize: number,
  serverPort: SettingMultiSource<number>,
  serverContextPath: SettingMultiSource<string>,
  koboProxy: boolean,
  koboPort?: number,
  kepubifyPath: SettingMultiSource<string>,
  prefetchPages: number,
}

export interface SettingMultiSource<T> {
  configurationSource?: T,
  databaseSource?: T,
  effectiveValue?: T,
}

export interface SettingsUpdateDto {
  registrationMode?: RegistrationMode,
  deleteEmptyCollections?: boolean,
  deleteEmptyReadLists?: boolean,
  rememberMeDurationDays?: number,
  renewRememberMeKey?: boolean,
  thumbnailSize?: ThumbnailSizeDto,
  taskPoolSize?: number,
  serverPort?: number,
  serverContextPath?: string,
  koboProxy?: boolean,
  koboPort?: number,
  kepubifyPath?: string,
  prefetchPages?: number,
}

export enum RegistrationMode {
  DISABLED = 'DISABLED',
  OPEN = 'OPEN',
  INVITE = 'INVITE',
}

export enum ThumbnailSizeDto {
  DEFAULT = 'DEFAULT',
  MEDIUM = 'MEDIUM',
  LARGE = 'LARGE',
  XLARGE = 'XLARGE',
}
