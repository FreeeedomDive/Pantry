package com.xdd.pantry.application.pantries

import com.xdd.pantry.application.users.RegisterUserUseCase
import com.xdd.pantry.domain.pantries.Pantry
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.TelegramUserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

sealed interface AcceptInviteResult {
    data class Joined(val pantry: Pantry) : AcceptInviteResult
    data class AlreadyMember(val pantry: Pantry) : AcceptInviteResult
    data object InvalidInvite : AcceptInviteResult
}

@Service
@Transactional
class AcceptPantryInviteUseCase(
    private val invites: PantryInviteRepository,
    private val pantries: PantryRepository,
    private val registerUser: RegisterUserUseCase,
) {
    fun acceptInvite(telegramUserId: TelegramUserId, token: UUID): AcceptInviteResult {
        val invite = invites.find(token) ?: return AcceptInviteResult.InvalidInvite
        if (invite.isExpired(Instant.now())) return AcceptInviteResult.InvalidInvite
        val pantry = pantries.getPantry(invite.pantryId) ?: return AcceptInviteResult.InvalidInvite

        val user = registerUser.findOrRegister(telegramUserId)
        if (pantries.getPantryMember(invite.pantryId, user.id) != null) {
            return AcceptInviteResult.AlreadyMember(pantry)
        }

        pantries.save(PantryMember(invite.pantryId, user.id, PantryRole.MEMBER, Instant.now()))
        return AcceptInviteResult.Joined(pantry)
    }
}
