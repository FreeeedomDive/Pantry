package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TransferPantryOwnershipUseCase(
    private val pantries: PantryRepository,
    private val pantryAccessGuard: PantryAccessGuard,
) {
    fun transferPantryOwnership(pantryId: PantryId, ownerId: UserId, memberId: UserId) {
        require(ownerId != memberId) { "Cannot transfer ownership to the current owner" }
        val oldOwner = pantryAccessGuard.checkAccess(pantryId, ownerId, shouldBeOwner = true)
        val newOwner = pantryAccessGuard.checkAccess(pantryId, memberId)
        pantries.updateRole(pantryId, oldOwner.userId, PantryRole.MEMBER)
        pantries.updateRole(pantryId, newOwner.userId, PantryRole.OWNER)
    }
}