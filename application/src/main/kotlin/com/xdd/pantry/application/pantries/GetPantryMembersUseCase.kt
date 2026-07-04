package com.xdd.pantry.application.pantries

import com.xdd.pantry.application.users.UserRepository
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.TelegramUserId
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import java.time.Instant

data class PantryMemberInfo(
    val telegramUserId: TelegramUserId,
    val role: PantryRole,
    val joinedAt: Instant,
)

@Service
class GetPantryMembersUseCase(
    private val pantries: PantryRepository,
    private val users: UserRepository,
    private val pantryAccessGuard: PantryAccessGuard,
) {
    fun getPantryMembers(userId: UserId, pantryId: PantryId): List<PantryMemberInfo> {
        pantryAccessGuard.checkAccess(pantryId, userId)
        val members = pantries.getPantryMembers(pantryId)
        val usersById = users.findByIds(members.map { it.userId }).associateBy { it.id }
        return members
            .map { member ->
                PantryMemberInfo(
                    telegramUserId = usersById.getValue(member.userId).telegramUserId,
                    role = member.role,
                    joinedAt = member.joinedAt,
                )
            }
            .sortedWith(compareBy({ it.role != PantryRole.OWNER }, { it.joinedAt }))
    }
}
