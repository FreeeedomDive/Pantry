package com.xdd.pantry.infrastructure.users

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class UserEntity(
    @Id var id: UUID,
    @Column(name = "telegram_user_id", nullable = false, unique = true)
    var telegramUserId: Long,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
    @Version var version: Long? = null,
)