package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.pantries.CannotDeleteLastPantryException
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class LeaveOrDeletePantryUseCase(
    private val pantries: PantryRepository,
    private val pantryAccessGuard: PantryAccessGuard,
    private val defaultPantryReassigner: DefaultPantryReassigner,
) {
    fun leaveOrDelete(userId: UserId, pantryId: PantryId) {
        val member = pantryAccessGuard.checkAccess(pantryId, userId)
        if (member.role == PantryRole.OWNER) {
            val userMemberships = pantries.getUserMemberships(userId);
            if (userMemberships.filter { it.role == PantryRole.OWNER }.size <= 1)
                throw CannotDeleteLastPantryException(userId)
            pantries.delete(pantryId)
        } else {
            pantries.deletePantryMember(pantryId, userId)
            defaultPantryReassigner.reassignIfNeeded(userId, pantryId)
        }
    }
}
