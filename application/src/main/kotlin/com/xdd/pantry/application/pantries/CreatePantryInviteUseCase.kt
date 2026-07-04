package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryInvite
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class CreatePantryInviteUseCase(
    private val invites: PantryInviteRepository,
    private val pantryAccessGuard: PantryAccessGuard,
) {
    fun createInvite(userId: UserId, pantryId: PantryId): PantryInvite {
        pantryAccessGuard.checkAccess(pantryId, userId, shouldBeOwner = true)
        val now = Instant.now()
        return invites.save(PantryInvite(UUID.randomUUID(), pantryId, userId, now, now.plus(INVITE_TTL)))
    }

    companion object {
        private val INVITE_TTL: Duration = Duration.ofDays(7)
    }
}
