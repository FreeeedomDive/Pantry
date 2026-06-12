package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.pantries.Pantry
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.UUID

class CreateNewPantryUseCaseTest {

    private val pantries = mockk<PantryRepository>()
    private val useCase = CreateNewPantryUseCase(pantries)

    @Test
    fun `creates pantry and registers creator as its owner`() {
        every { pantries.save(any<Pantry>()) } answers { firstArg() }
        every { pantries.save(any<PantryMember>()) } answers { firstArg() }
        val creatorId = UserId(UUID.randomUUID())

        val pantry = useCase.createNewPantry(creatorId, "Наша квартира")

        pantry.name shouldBe "Наша квартира"
        verify(exactly = 1) { pantries.save(any<Pantry>()) }
        verify(exactly = 1) {
            pantries.save(match<PantryMember> {
                it.pantryId == pantry.id && it.userId == creatorId && it.role == PantryRole.OWNER
            })
        }
    }
}
