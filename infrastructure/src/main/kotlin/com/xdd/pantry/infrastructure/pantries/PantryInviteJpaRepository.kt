package com.xdd.pantry.infrastructure.pantries

import com.xdd.pantry.application.pantries.PantryInviteRepository
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryInvite
import com.xdd.pantry.domain.users.UserId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface PantryInviteJpaRepository : JpaRepository<PantryInviteEntity, UUID>

@Repository
class PantryInviteRepositoryAdapter(
    private val invites: PantryInviteJpaRepository,
) : PantryInviteRepository {

    override fun save(invite: PantryInvite): PantryInvite =
        invites.save(invite.toEntity()).toDomain()

    override fun find(token: UUID): PantryInvite? =
        invites.findById(token).map { it.toDomain() }.orElse(null)
}

private fun PantryInviteEntity.toDomain() =
    PantryInvite(token, PantryId(pantryId), UserId(createdBy), createdAt, expiresAt)

private fun PantryInvite.toEntity() =
    PantryInviteEntity(token, pantryId.value, createdBy.value, createdAt, expiresAt)
