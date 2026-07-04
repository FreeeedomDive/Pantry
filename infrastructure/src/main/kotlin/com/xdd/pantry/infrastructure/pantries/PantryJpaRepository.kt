package com.xdd.pantry.infrastructure.pantries

import com.xdd.pantry.application.pantries.PantryRepository
import com.xdd.pantry.domain.pantries.Pantry
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

interface PantryJpaRepository : JpaRepository<PantryEntity, UUID> {
    @Query(
        """
        select p from PantryEntity p
        join PantryMemberEntity m on m.id.pantryId = p.id
        where m.id.userId = :userId
        """
    )
    fun findAllByMemberUserId(@Param("userId") userId: UUID): List<PantryEntity>
}

interface PantryMemberJpaRepository : JpaRepository<PantryMemberEntity, PantryMemberKey> {
    fun findByIdPantryId(pantryId: UUID): List<PantryMemberEntity>
    fun findByIdUserId(userId: UUID): List<PantryMemberEntity>
}

@Repository
class PantryRepositoryAdapter(
    private val pantries: PantryJpaRepository,
    private val pantryMembers: PantryMemberJpaRepository
) : PantryRepository {
    override fun getPantry(pantryId: PantryId): Pantry? {
        val pantryEntity = pantries.findById(pantryId.value).orElse(null)
        return pantryEntity?.toDomain()
    }

    override fun getUserPantries(userId: UserId): List<Pantry> =
        pantries.findAllByMemberUserId(userId.value).map { it.toDomain() }

    override fun getPantryMember(
        pantryId: PantryId,
        userId: UserId
    ): PantryMember? {
        return pantryMembers.findById(PantryMemberKey(pantryId.value, userId.value)).map { it.toDomain() }.orElse(null)
    }

    override fun getPantryMembers(pantryId: PantryId): List<PantryMember> {
        return pantryMembers.findByIdPantryId(pantryId.value).map { it.toDomain() }
    }

    override fun getUserMemberships(userId: UserId): List<PantryMember> {
        return pantryMembers.findByIdUserId(userId.value).map { it.toDomain() }
    }

    override fun save(pantry: Pantry): Pantry = pantries.save(pantry.toEntity()).toDomain()

    override fun save(newMember: PantryMember): PantryMember {
        return pantryMembers.save(newMember.toEntity()).toDomain()
    }

    override fun rename(pantryId: PantryId, newName: String) {
        val current = pantries.findById(pantryId.value)
        current.ifPresent {
            it.name = newName
            pantries.save(it)
        }
    }

    override fun updateRole(
        pantryId: PantryId,
        userId: UserId,
        newRole: PantryRole
    ) {
        val current = pantryMembers.findById(PantryMemberKey(pantryId.value, userId.value))
        current.ifPresent {
            it.role = newRole.name
            pantryMembers.save(it)
        }
    }

    override fun delete(pantryId: PantryId) {
        pantries.deleteById(pantryId.value)
    }

    override fun deletePantryMember(pantryId: PantryId, userId: UserId) {
        pantryMembers.deleteById(PantryMemberKey(pantryId.value, userId.value))
    }
}

private fun PantryEntity.toDomain() = Pantry(PantryId(id), name, createdAt)
private fun Pantry.toEntity() = PantryEntity(id.value, name, createdAt)
private fun PantryMemberEntity.toDomain() = PantryMember(PantryId(id.pantryId), UserId(id.userId), PantryRole.valueOf(role), joinedAt)
private fun PantryMember.toEntity() = PantryMemberEntity(PantryMemberKey(pantryId.value, userId.value), role.name, joinedAt)