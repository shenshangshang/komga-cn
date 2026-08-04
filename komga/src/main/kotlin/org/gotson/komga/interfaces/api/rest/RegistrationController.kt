package org.gotson.komga.interfaces.api.rest

import jakarta.validation.Valid
import org.gotson.komga.domain.model.KomgaUser
import org.gotson.komga.domain.model.UserEmailAlreadyExistsException
import org.gotson.komga.domain.model.UserRoles
import org.gotson.komga.domain.service.KomgaUserLifecycle
import org.gotson.komga.infrastructure.configuration.KomgaSettingsProvider
import org.gotson.komga.infrastructure.configuration.RegistrationMode
import org.gotson.komga.infrastructure.jooq.main.UserInvitationDao
import org.gotson.komga.infrastructure.security.KomgaPrincipal
import org.gotson.komga.interfaces.api.rest.dto.InvitationCreationDto
import org.gotson.komga.interfaces.api.rest.dto.InvitationDto
import org.gotson.komga.interfaces.api.rest.dto.RegistrationRequestDto
import org.gotson.komga.interfaces.api.rest.dto.RegistrationStatusDto
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@RestController
class RegistrationController(
  private val settings: KomgaSettingsProvider,
  private val userLifecycle: KomgaUserLifecycle,
  private val invitations: UserInvitationDao,
) {
  @GetMapping("api/v1/registration")
  fun status() = RegistrationStatusDto(settings.registrationMode)

  @PostMapping("api/v1/registration")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  fun register(
    @Valid @RequestBody request: RegistrationRequestDto,
  ) {
    when (settings.registrationMode) {
      RegistrationMode.DISABLED -> throw ResponseStatusException(HttpStatus.FORBIDDEN, "Registration is disabled")
      RegistrationMode.OPEN -> Unit
      RegistrationMode.INVITE -> {
        val token =
          request.invitationToken?.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid invitation is required")
        if (!invitations.isValid(token)) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation is invalid or expired")
      }
    }

    try {
      userLifecycle.createUser(
        KomgaUser(
          email = request.email,
          password = request.password,
          roles = setOf(UserRoles.FILE_DOWNLOAD, UserRoles.PAGE_STREAMING),
          sharedAllLibraries = false,
        ),
      )
      if (settings.registrationMode == RegistrationMode.INVITE && !invitations.consume(request.invitationToken!!)) {
        throw ResponseStatusException(HttpStatus.CONFLICT, "Invitation has already been used")
      }
    } catch (_: UserEmailAlreadyExistsException) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration could not be completed")
    }
  }

  @RequestMapping("api/v1/invitations")
  @PreAuthorize("hasRole('ADMIN')")
  @RestController
  class InvitationController(
    private val invitations: UserInvitationDao,
  ) {
    @GetMapping
    fun list(): List<InvitationDto> = invitations.findAll().map { it.toDto() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
      @AuthenticationPrincipal principal: KomgaPrincipal,
      @Valid @RequestBody request: InvitationCreationDto,
    ): InvitationDto {
      val created = invitations.create(principal.user.id, LocalDateTime.now().plusDays(request.expiresInDays))
      return created.invitation.toDto(created.token)
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(
      @PathVariable id: String,
    ) {
      if (!invitations.revoke(id)) throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
  }
}

private fun org.gotson.komga.infrastructure.jooq.main.UserInvitation.toDto(token: String? = null) = InvitationDto(id, createdBy, createdDate, expiresDate, usedDate, revokedDate, token)
