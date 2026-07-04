package com.xdd.pantry.application.pantries

import com.xdd.pantry.application.users.UserDefaultsRepository
import com.xdd.pantry.domain.pantries.Pantry
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class UserPantry(val pantry: Pantry, val role: PantryRole, val isDefault: Boolean)

@Service
@Transactional(readOnly = true)
class GetUserPantriesUseCase(
    private val pantries: PantryRepository,
    private val userDefaults: UserDefaultsRepository,
) {
    fun getUserPantries(userId: UserId): List<UserPantry> {
        val roleByPantry = pantries.getUserMemberships(userId).associate { it.pantryId to it.role }
        val userPantries = pantries.getUserPantries(userId)
        val defaultPantryId = userDefaults.getDefaultPantryId(userId) ?: userPantries.firstOrNull()?.id
        return userPantries.map { pantry ->
            UserPantry(pantry, roleByPantry.getValue(pantry.id), pantry.id == defaultPantryId)
        }
    }
}
