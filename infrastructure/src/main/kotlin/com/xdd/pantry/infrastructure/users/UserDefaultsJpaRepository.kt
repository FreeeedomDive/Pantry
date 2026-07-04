package com.xdd.pantry.infrastructure.users

import com.xdd.pantry.application.users.UserDefaultsRepository
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.users.UserId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface UserDefaultsJpaRepository : JpaRepository<UserDefaultsEntity, UUID>

@Repository
class UserDefaultsRepositoryAdapter(
    private val jpa: UserDefaultsJpaRepository,
) : UserDefaultsRepository {

    override fun getDefaultPantryId(userId: UserId): PantryId? =
        jpa.findById(userId.value).orElse(null)?.defaultPantryId?.let { PantryId(it) }

    @Transactional
    override fun setDefaultPantryId(userId: UserId, pantryId: PantryId) {
        val existing = jpa.findById(userId.value).orElse(null)
        if (existing != null) {
            existing.defaultPantryId = pantryId.value
        } else {
            jpa.save(UserDefaultsEntity(userId.value, pantryId.value))
        }
    }
}
