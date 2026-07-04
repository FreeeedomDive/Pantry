package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.pantries.Pantry
import com.xdd.pantry.domain.pantries.PantryActionDeniedException
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class RenamePantryUseCaseTest {

    private val pantries = mockk<PantryRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val useCase = RenamePantryUseCase(pantries, guard)

    private val pantryId = PantryId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())

    @Test
    fun `renames the pantry as owner`() {
        every { guard.checkAccess(pantryId, userId, shouldBeOwner = true) } returns owner()
        justRun { pantries.rename(pantryId, "Дача") }
        every { pantries.getPantry(pantryId) } returns Pantry(pantryId, "Дача", Instant.now())

        val renamed = useCase.renamePantry(userId, pantryId, "  Дача  ")

        renamed.name shouldBe "Дача"
        verify(exactly = 1) { pantries.rename(pantryId, "Дача") }
    }

    @Test
    fun `rejects a blank name`() {
        shouldThrow<IllegalArgumentException> { useCase.renamePantry(userId, pantryId, "   ") }

        verify(exactly = 0) { pantries.rename(any(), any()) }
    }

    @Test
    fun `denies rename for a non-owner`() {
        every { guard.checkAccess(pantryId, userId, shouldBeOwner = true) } throws
            PantryActionDeniedException(userId, pantryId)

        shouldThrow<PantryActionDeniedException> { useCase.renamePantry(userId, pantryId, "Дача") }

        verify(exactly = 0) { pantries.rename(any(), any()) }
    }

    private fun owner() = PantryMember(pantryId, userId, PantryRole.OWNER, Instant.now())
}
