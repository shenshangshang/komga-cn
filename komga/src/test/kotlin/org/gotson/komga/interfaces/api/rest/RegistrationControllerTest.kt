package org.gotson.komga.interfaces.api.rest

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.domain.model.KomgaUser
import org.gotson.komga.domain.persistence.KomgaUserRepository
import org.gotson.komga.domain.service.KomgaUserLifecycle
import org.gotson.komga.infrastructure.configuration.KomgaSettingsProvider
import org.gotson.komga.infrastructure.configuration.RegistrationMode
import org.gotson.komga.infrastructure.jooq.main.UserInvitationDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.put
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@ActiveProfiles("test")
class RegistrationControllerTest(
  @Autowired private val mockMvc: MockMvc,
  @Autowired private val settings: KomgaSettingsProvider,
  @Autowired private val users: KomgaUserRepository,
  @Autowired private val userLifecycle: KomgaUserLifecycle,
  @Autowired private val objectMapper: ObjectMapper,
  @Autowired private val invitations: UserInvitationDao,
) {
  @BeforeAll
  fun createAdmin() {
    users.insert(KomgaUser("admin@example.org", "password", id = "0"))
  }

  @AfterEach
  fun cleanup() {
    settings.registrationMode = RegistrationMode.DISABLED
    invitations.findAll().forEach { invitations.delete(it.id) }
    users.findAll().filterNot { it.id == "0" }.forEach { userLifecycle.deleteUser(it) }
  }

  @Test
  fun `registration status is public and registration is disabled by default`() {
    mockMvc.get("/api/v1/registration").andExpect {
      status { isOk() }
      jsonPath("mode") { value("DISABLED") }
    }
    mockMvc.post("/api/v1/registration") {
      contentType = MediaType.APPLICATION_JSON
      content = """{"email":"new@example.org","password":"password"}"""
    }.andExpect { status { isForbidden() } }
  }

  @Test
  fun `open registration creates least privilege user`() {
    settings.registrationMode = RegistrationMode.OPEN
    mockMvc.post("/api/v1/registration") {
      contentType = MediaType.APPLICATION_JSON
      content = """{"email":"open@example.org","password":"password"}"""
    }.andExpect { status { isCreated() } }

    val user = users.findByEmailIgnoreCaseOrNull("open@example.org")!!
    assertThat(user.sharedAllLibraries).isFalse()
    assertThat(user.roles.map { it.name }).containsExactlyInAnyOrder("FILE_DOWNLOAD", "PAGE_STREAMING")
  }

  @Test
  @WithMockCustomUser(id = "0", roles = ["ADMIN"])
  fun `invitation can be used only once`() {
    settings.registrationMode = RegistrationMode.INVITE
    val invitationResponse = mockMvc.post("/api/v1/invitations") {
      contentType = MediaType.APPLICATION_JSON
      content = """{"expiresInDays":7}"""
    }.andExpect { status { isCreated() } }.andReturn()
    val token = objectMapper.readTree(invitationResponse.response.contentAsString).get("token").asText()

    mockMvc.get("/api/v1/registration/invitation") {
      param("token", token)
    }.andExpect {
      status { isOk() }
      jsonPath("valid") { value(true) }
    }

    mockMvc.post("/api/v1/registration") {
      contentType = MediaType.APPLICATION_JSON
      content = """{"email":"invited@example.org","password":"password","invitationToken":"  $token  "}"""
    }.andExpect { status { isCreated() } }

    mockMvc.get("/api/v1/registration/invitation") {
      param("token", token)
    }.andExpect {
      status { isOk() }
      jsonPath("valid") { value(false) }
    }
    mockMvc.post("/api/v1/registration") {
      contentType = MediaType.APPLICATION_JSON
      content = """{"email":"second@example.org","password":"password","invitationToken":"$token"}"""
    }.andExpect { status { isBadRequest() } }
  }

  @Test
  @WithMockCustomUser(id = "0", roles = ["ADMIN"])
  fun `invitation can be deleted and becomes invalid`() {
    settings.registrationMode = RegistrationMode.INVITE
    val invitationResponse = mockMvc.post("/api/v1/invitations") {
      contentType = MediaType.APPLICATION_JSON
      content = """{"expiresInDays":7}"""
    }.andExpect { status { isCreated() } }.andReturn()
    val token = objectMapper.readTree(invitationResponse.response.contentAsString).get("token").asText()
    val id = objectMapper.readTree(invitationResponse.response.contentAsString).get("id").asText()

    // Token should be valid before deletion
    mockMvc.get("/api/v1/registration/invitation") {
      param("token", token)
    }.andExpect {
      status { isOk() }
      jsonPath("valid") { value(true) }
    }

    // Delete the invitation
    mockMvc.delete("/api/v1/invitations/$id").andExpect { status { isNoContent() } }

    // Token should be invalid after deletion
    mockMvc.get("/api/v1/registration/invitation") {
      param("token", token)
    }.andExpect {
      status { isOk() }
      jsonPath("valid") { value(false) }
    }

    // Registration with deleted token should fail
    mockMvc.post("/api/v1/registration") {
      contentType = MediaType.APPLICATION_JSON
      content = """{"email":"deleted@example.org","password":"password","invitationToken":"$token"}"""
    }.andExpect { status { isBadRequest() } }
  }

  @Test
  @WithMockCustomUser(id = "0", roles = ["ADMIN"])
  fun `used invitation can be deleted`() {
    settings.registrationMode = RegistrationMode.INVITE
    val invitationResponse = mockMvc.post("/api/v1/invitations") {
      contentType = MediaType.APPLICATION_JSON
      content = """{"expiresInDays":7}"""
    }.andExpect { status { isCreated() } }.andReturn()
    val token = objectMapper.readTree(invitationResponse.response.contentAsString).get("token").asText()
    val id = objectMapper.readTree(invitationResponse.response.contentAsString).get("id").asText()

    // Use the invitation
    mockMvc.post("/api/v1/registration") {
      contentType = MediaType.APPLICATION_JSON
      content = """{"email":"used@example.org","password":"password","invitationToken":"$token"}"""
    }.andExpect { status { isCreated() } }

    // Delete the used invitation - should still succeed
    mockMvc.delete("/api/v1/invitations/$id").andExpect { status { isNoContent() } }

    // Deleting again should return 404
    mockMvc.delete("/api/v1/invitations/$id").andExpect { status { isNotFound() } }
  }

  @Test
  @WithMockCustomUser(id = "0", roles = ["ADMIN"])
  fun `deleting non-existent invitation returns 404`() {
    mockMvc.delete("/api/v1/invitations/nonexistent").andExpect { status { isNotFound() } }
  }

  @Test
  @WithMockCustomUser(id = "0", roles = ["ADMIN"])
  fun `invitation can be revoked and becomes invalid`() {
    settings.registrationMode = RegistrationMode.INVITE
    val invitationResponse = mockMvc.post("/api/v1/invitations") {
      contentType = MediaType.APPLICATION_JSON
      content = """{"expiresInDays":7}"""
    }.andExpect { status { isCreated() } }.andReturn()
    val token = objectMapper.readTree(invitationResponse.response.contentAsString).get("token").asText()
    val id = objectMapper.readTree(invitationResponse.response.contentAsString).get("id").asText()

    mockMvc.put("/api/v1/invitations/$id/revoke").andExpect { status { isNoContent() } }

    mockMvc.get("/api/v1/registration/invitation") {
      param("token", token)
    }.andExpect {
      status { isOk() }
      jsonPath("valid") { value(false) }
    }

    mockMvc.post("/api/v1/registration") {
      contentType = MediaType.APPLICATION_JSON
      content = """{"email":"revoked@example.org","password":"password","invitationToken":"$token"}"""
    }.andExpect { status { isBadRequest() } }
  }

  @Test
  @WithMockCustomUser(id = "0", roles = ["ADMIN"])
  fun `revoking non-existent invitation returns 404`() {
    mockMvc.put("/api/v1/invitations/nonexistent/revoke").andExpect { status { isNotFound() } }
  }

  @Test
  @WithMockCustomUser(id = "0", roles = ["ADMIN"])
  fun `revoked invitation can be deleted`() {
    settings.registrationMode = RegistrationMode.INVITE
    val invitationResponse = mockMvc.post("/api/v1/invitations") {
      contentType = MediaType.APPLICATION_JSON
      content = """{"expiresInDays":7}"""
    }.andExpect { status { isCreated() } }.andReturn()
    val id = objectMapper.readTree(invitationResponse.response.contentAsString).get("id").asText()

    mockMvc.put("/api/v1/invitations/$id/revoke").andExpect { status { isNoContent() } }
    mockMvc.delete("/api/v1/invitations/$id").andExpect { status { isNoContent() } }
    mockMvc.delete("/api/v1/invitations/$id").andExpect { status { isNotFound() } }
  }
}
