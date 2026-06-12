package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.pantries.PantryActionDeniedException
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class TransferPantryOwnershipUseCaseTest {

    private val pantries = mockk<PantryRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val useCase = TransferPantryOwnershipUseCase(pantries, guard)

    private val pantryId = PantryId(UUID.randomUUID())
    private val ownerId = UserId(UUID.randomUUID())
    private val memberId = UserId(UUID.randomUUID())

    @Test
    fun `swaps roles between current owner and member`() {
        every { guard.checkAccess(pantryId, ownerId, true) } returns member(ownerId, PantryRole.OWNER)
        every { guard.checkAccess(pantryId, memberId, false) } returns member(memberId, PantryRole.MEMBER)
        justRun { pantries.updateRole(any(), any(), any()) }

        useCase.transferPantryOwnership(pantryId, ownerId, memberId)

        verifyOrder {
            pantries.updateRole(pantryId, ownerId, PantryRole.MEMBER)
            pantries.updateRole(pantryId, memberId, PantryRole.OWNER)
        }
    }

    @Test
    fun `cannot transfer ownership to oneself`() {
        shouldThrow<IllegalArgumentException> {
            useCase.transferPantryOwnership(pantryId, ownerId, ownerId)
        }

        verify(exactly = 0) { pantries.updateRole(any(), any(), any()) }
    }

    @Test
    fun `propagates denial when target is not a member`() {
        every { guard.checkAccess(pantryId, ownerId, true) } returns member(ownerId, PantryRole.OWNER)
        every { guard.checkAccess(pantryId, memberId, false) } throws
            PantryActionDeniedException(memberId, pantryId)

        shouldThrow<PantryActionDeniedException> {
            useCase.transferPantryOwnership(pantryId, ownerId, memberId)
        }

        verify(exactly = 0) { pantries.updateRole(any(), any(), any()) }
    }

    private fun member(userId: UserId, role: PantryRole) =
        PantryMember(pantryId, userId, role, Instant.now())
}
