package com.xdd.pantry.application.users

import com.xdd.pantry.domain.users.TelegramUserId
import com.xdd.pantry.domain.users.User
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class RegisterUserUseCase(private val users: UserRepository) {
    @Transactional
    fun findOrRegister(telegramUserId: TelegramUserId): User =
        users.findByTelegramUserId(telegramUserId)
            ?: users.save(User(UserId(UUID.randomUUID()), telegramUserId, Instant.now()))
}