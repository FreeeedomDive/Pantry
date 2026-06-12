package com.xdd.pantry.infrastructure.users

import com.xdd.pantry.application.users.UserRepository
import com.xdd.pantry.domain.users.TelegramUserId
import com.xdd.pantry.domain.users.User
import com.xdd.pantry.domain.users.UserId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface UserJpaRepository : JpaRepository<UserEntity, UUID> {
    fun findByTelegramUserId(telegramUserId: Long): UserEntity?
}

@Repository
class UserRepositoryAdapter(
    private val jpa: UserJpaRepository,
) : UserRepository {

    override fun findByTelegramUserId(telegramUserId: TelegramUserId): User? =
        jpa.findByTelegramUserId(telegramUserId.value)?.toDomain()

    override fun save(user: User): User =
        jpa.save(user.toEntity()).toDomain()
}

private fun UserEntity.toDomain() = User(UserId(id), TelegramUserId(telegramUserId), createdAt)
private fun User.toEntity() = UserEntity(id.value, telegramUserId.value, createdAt)
