package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class KickPantryMemberUseCase(
    private val pantries: PantryRepository,
    private val pantryAccessGuard: PantryAccessGuard
) {
    fun kickPantryMember(pantryId: PantryId, ownerId: UserId, memberId: UserId) {
        require(ownerId != memberId) { "Owner cannot kick themselves out of the pantry" }
        pantryAccessGuard.checkAccess(pantryId, ownerId, shouldBeOwner = true)
        pantryAccessGuard.checkAccess(pantryId, memberId)
        pantries.deletePantryMember(pantryId, memberId)
    }
}