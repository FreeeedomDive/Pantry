package com.xdd.pantry.application.pantries

import com.xdd.pantry.application.users.UserDefaultsRepository
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SetDefaultPantryUseCase(
    private val pantryAccessGuard: PantryAccessGuard,
    private val userDefaults: UserDefaultsRepository,
) {
    fun setDefault(userId: UserId, pantryId: PantryId) {
        pantryAccessGuard.checkAccess(pantryId, userId)
        userDefaults.setDefaultPantryId(userId, pantryId)
    }
}
