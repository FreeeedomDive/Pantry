package com.xdd.pantry.infrastructure.pantries

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "pantry_invites")
class PantryInviteEntity(
    @Id var token: UUID,
    @Column(name = "pantry_id", nullable = false) var pantryId: UUID,
    @Column(name = "created_by", nullable = false) var createdBy: UUID,
    @Column(name = "created_at", nullable = false) var createdAt: Instant,
    @Column(name = "expires_at", nullable = false) var expiresAt: Instant,
    @Version var version: Long? = null,
)
