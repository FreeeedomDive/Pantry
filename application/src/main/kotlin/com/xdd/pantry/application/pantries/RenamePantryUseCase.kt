package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.pantries.Pantry
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RenamePantryUseCase(
    private val pantries: PantryRepository,
    private val pantryAccessGuard: PantryAccessGuard,
) {
    fun renamePantry(userId: UserId, pantryId: PantryId, newName: String): Pantry {
        require(newName.isNotBlank()) { "Pantry name must not be blank" }
        pantryAccessGuard.checkAccess(pantryId, userId, shouldBeOwner = true)
        pantries.rename(pantryId, newName.trim())
        return checkNotNull(pantries.getPantry(pantryId)) { "Pantry $pantryId disappeared while renaming" }
    }
}
