package org.gotson.komga.interfaces.api.rest

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.domain.model.KomgaUser
import org.gotson.komga.domain.persistence.KomgaUserRepository
import org.gotson.komga.domain.service.KomgaUserLifecycle
import org.gotson.komga.infrastructure.configuration.KomgaSettingsProvider
import org.gotson.komga.infrastructure.configuration.RegistrationMode
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
) {
  @BeforeAll
  fun createAdmin() {
    users.insert(KomgaUser("admin@example.org", "password", id = "0"))
  }

  @AfterEach
  fun cleanup() {
    settings.registrationMode = RegistrationMode.DISABLED
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

    mockMvc.post("/api/v1/registration") {
      contentType = MediaType.APPLICATION_JSON
      content = """{"email":"invited@example.org","password":"password","invitationToken":"$token"}"""
    }.andExpect { status { isCreated() } }
    mockMvc.post("/api/v1/registration") {
      contentType = MediaType.APPLICATION_JSON
      content = """{"email":"second@example.org","password":"password","invitationToken":"$token"}"""
    }.andExpect { status { isBadRequest() } }
  }
}
