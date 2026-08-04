package org.gotson.komga.interfaces.api.rest.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.gotson.komga.infrastructure.configuration.RegistrationMode
import java.time.LocalDateTime

data class RegistrationStatusDto(
  val mode: RegistrationMode,
)

data class RegistrationRequestDto(
  @get:Email(regexp = ".+@.+\\..+") val email: String,
  @get:NotBlank @get:Size(min = 8, max = 255) val password: String,
  val invitationToken: String? = null,
)

data class InvitationCreationDto(
  @get:Min(1) @get:Max(30) val expiresInDays: Long = 7,
)

data class InvitationDto(
  val id: String,
  val createdBy: String,
  val createdDate: LocalDateTime,
  val expiresDate: LocalDateTime,
  val usedDate: LocalDateTime?,
  val revokedDate: LocalDateTime?,
  val token: String? = null,
)
