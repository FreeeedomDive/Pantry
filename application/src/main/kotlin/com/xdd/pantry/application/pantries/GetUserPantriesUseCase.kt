package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.pantries.Pantry
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetUserPantriesUseCase(
    private val pantries: PantryRepository,
) {
    fun getUserPantries(userId: UserId): List<Pantry> = pantries.getUserPantries(userId)
}
