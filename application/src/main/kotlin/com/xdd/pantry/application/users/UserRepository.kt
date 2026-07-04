package com.xdd.pantry.application.users

import com.xdd.pantry.domain.users.TelegramUserId
import com.xdd.pantry.domain.users.User
import com.xdd.pantry.domain.users.UserId

interface UserRepository {
    fun findByTelegramUserId(telegramUserId: TelegramUserId): User?
    fun findByIds(ids: Collection<UserId>): List<User>
    fun save(user: User): User
}