package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class AddPantryMemberUseCase(
    private val pantries: PantryRepository,
    private val pantryAccessGuard: PantryAccessGuard,
) {
    fun addPantryMember(pantryId: PantryId, userId: UserId, newMemberId: UserId) {
        pantryAccessGuard.checkAccess(pantryId, userId, shouldBeOwner = true)
        val member = pantries.getPantryMember(pantryId, newMemberId)
        if (member != null) {
            return
        }
        val newMember = PantryMember(pantryId, newMemberId, PantryRole.MEMBER, Instant.now())
        pantries.save(newMember)
    }
}