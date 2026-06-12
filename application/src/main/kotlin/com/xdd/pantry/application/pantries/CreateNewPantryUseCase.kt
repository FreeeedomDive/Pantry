package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.pantries.Pantry
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class CreateNewPantryUseCase(private val pantries: PantryRepository) {
    fun createNewPantry(userId: UserId, pantryName: String): Pantry {
        val pantry = Pantry(PantryId(UUID.randomUUID()), pantryName, Instant.now())
        val member = PantryMember(pantry.id, userId, PantryRole.OWNER, Instant.now())
        pantries.save(pantry)
        pantries.save(member)
        return pantry
    }
}