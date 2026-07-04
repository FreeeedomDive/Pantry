package com.xdd.pantry.application.pantries

import com.xdd.pantry.application.users.UserDefaultsRepository
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service

@Service
class DefaultPantryReassigner(
    private val pantries: PantryRepository,
    private val userDefaults: UserDefaultsRepository,
) {
    fun reassignIfNeeded(userId: UserId, removedPantryId: PantryId) {
        if (userDefaults.getDefaultPantryId(userId) != removedPantryId) return
        val fallback = pantries.getUserPantries(userId).firstOrNull()
        if (fallback != null) userDefaults.setDefaultPantryId(userId, fallback.id)
        else userDefaults.clearDefaultPantryId(userId)
    }
}
