package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.pantries.Pantry
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class UserPantry(val pantry: Pantry, val role: PantryRole)

@Service
@Transactional(readOnly = true)
class GetUserPantriesUseCase(
    private val pantries: PantryRepository,
) {
    fun getUserPantries(userId: UserId): List<UserPantry> {
        val roleByPantry = pantries.getUserMemberships(userId).associate { it.pantryId to it.role }
        return pantries.getUserPantries(userId).map { pantry ->
            UserPantry(pantry, roleByPantry.getValue(pantry.id))
        }
    }
}
