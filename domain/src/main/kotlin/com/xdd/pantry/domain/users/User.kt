package com.xdd.pantry.domain.users

import java.time.Instant
import java.util.UUID

@JvmInline value class UserId(val value: UUID)
@JvmInline value class TelegramUserId(val value: Long)

data class User(
    val id: UserId,
    val telegramUserId: TelegramUserId,
    val createdAt: Instant,
)
