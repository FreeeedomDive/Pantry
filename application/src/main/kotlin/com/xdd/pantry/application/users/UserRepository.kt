package com.xdd.pantry.application.users

import com.xdd.pantry.domain.users.TelegramUserId
import com.xdd.pantry.domain.users.User

interface UserRepository {
    fun findByTelegramUserId(telegramUserId: TelegramUserId): User?
    fun save(user: User): User
}