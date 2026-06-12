package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.pantries.PantryActionDeniedException
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service

@Service
class PantryAccessGuard(private val pantries: PantryRepository) {
    fun checkAccess(pantryId: PantryId, userId: UserId, shouldBeOwner: Boolean = false): PantryMember {
        val member = pantries.getPantryMember(pantryId, userId)
        if (member == null || (shouldBeOwner && member.role != PantryRole.OWNER)) {
            throw PantryActionDeniedException(userId, pantryId)
        }

        return member
    }
}