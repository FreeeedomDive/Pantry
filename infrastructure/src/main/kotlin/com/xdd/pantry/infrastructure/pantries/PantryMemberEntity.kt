package com.xdd.pantry.infrastructure.pantries

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Embeddable
data class PantryMemberKey(
    @Column(name = "pantry_id") var pantryId: UUID,
    @Column(name = "user_id") var userId: UUID
) : Serializable

@Entity
@Table(name = "pantry_members")
class PantryMemberEntity(
    @EmbeddedId var id: PantryMemberKey,
    @Column(name = "role", nullable = false) var role: String,
    @Column(name = "joined_at", nullable = false) var joinedAt: Instant,
    @Version var version: Long? = null,
)