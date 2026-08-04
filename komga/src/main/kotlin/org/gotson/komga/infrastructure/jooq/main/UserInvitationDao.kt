package org.gotson.komga.infrastructure.jooq.main

import com.github.f4b6a3.tsid.TsidCreator
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.RandomStringUtils
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.time.LocalDateTime

data class UserInvitation(
  val id: String,
  val createdBy: String,
  val createdDate: LocalDateTime,
  val expiresDate: LocalDateTime,
  val usedDate: LocalDateTime?,
  val revokedDate: LocalDateTime?,
)

data class CreatedUserInvitation(
  val invitation: UserInvitation,
  val token: String,
)

@Component
class UserInvitationDao(
  private val dsl: DSLContext,
) {
  fun create(
    createdBy: String,
    expiresDate: LocalDateTime,
  ): CreatedUserInvitation {
    val id = TsidCreator.getTsid256().toString()
    val token = RandomStringUtils.secure().nextAlphanumeric(48)
    val tokenHash = DigestUtils.sha256Hex(token)
    val now = LocalDateTime.now()
    dsl.execute(
      "insert into USER_INVITATION (ID, TOKEN_HASH, CREATED_BY, CREATED_DATE, EXPIRES_DATE) values (?, ?, ?, ?, ?)",
      id,
      tokenHash,
      createdBy,
      now,
      expiresDate,
    )
    return CreatedUserInvitation(UserInvitation(id, createdBy, now, expiresDate, null, null), token)
  }

  fun findAll(): List<UserInvitation> =
    dsl
      .fetch(
        "select ID, CREATED_BY, CREATED_DATE, EXPIRES_DATE, USED_DATE, REVOKED_DATE from USER_INVITATION order by CREATED_DATE desc",
      ).map {
        UserInvitation(
          id = it.get("ID", String::class.java)!!,
          createdBy = it.get("CREATED_BY", String::class.java)!!,
          createdDate = it.get("CREATED_DATE", LocalDateTime::class.java)!!,
          expiresDate = it.get("EXPIRES_DATE", LocalDateTime::class.java)!!,
          usedDate = it.get("USED_DATE", LocalDateTime::class.java),
          revokedDate = it.get("REVOKED_DATE", LocalDateTime::class.java),
        )
      }

  fun revoke(id: String): Boolean =
    dsl.execute(
      "update USER_INVITATION set REVOKED_DATE = ? where ID = ? and USED_DATE is null and REVOKED_DATE is null",
      LocalDateTime.now(),
      id,
    ) == 1

  fun isValid(token: String): Boolean =
    dsl.fetchExists(
      dsl
        .selectOne()
        .from("USER_INVITATION")
        .where("TOKEN_HASH = ? and USED_DATE is null and REVOKED_DATE is null and EXPIRES_DATE > ?", DigestUtils.sha256Hex(token), LocalDateTime.now()),
    )

  fun consume(token: String): Boolean {
    val now = LocalDateTime.now()
    return dsl.execute(
      "update USER_INVITATION set USED_DATE = ? where TOKEN_HASH = ? and USED_DATE is null and REVOKED_DATE is null and EXPIRES_DATE > ?",
      now,
      DigestUtils.sha256Hex(token),
      now,
    ) == 1
  }
}
