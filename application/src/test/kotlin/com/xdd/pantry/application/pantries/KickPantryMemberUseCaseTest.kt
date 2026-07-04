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
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class KickPantryMemberUseCaseTest {

    private val pantries = mockk<PantryRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val defaultPantryReassigner = mockk<DefaultPantryReassigner>()
    private val useCase = KickPantryMemberUseCase(pantries, guard, defaultPantryReassigner)

    private val pantryId = PantryId(UUID.randomUUID())
    private val ownerId = UserId(UUID.randomUUID())
    private val memberId = UserId(UUID.randomUUID())

    @Test
    fun `kicks member from pantry and reassigns their default if needed`() {
        every { guard.checkAccess(pantryId, ownerId, true) } returns member(ownerId, PantryRole.OWNER)
        every { guard.checkAccess(pantryId, memberId, false) } returns member(memberId, PantryRole.MEMBER)
        justRun { pantries.deletePantryMember(pantryId, memberId) }
        justRun { defaultPantryReassigner.reassignIfNeeded(memberId, pantryId) }

        useCase.kickPantryMember(pantryId, ownerId, memberId)

        verify(exactly = 1) { pantries.deletePantryMember(pantryId, memberId) }
        verify(exactly = 1) { defaultPantryReassigner.reassignIfNeeded(memberId, pantryId) }
    }

    @Test
    fun `owner cannot kick themselves`() {
        shouldThrow<IllegalArgumentException> {
            useCase.kickPantryMember(pantryId, ownerId, ownerId)
        }

        verify(exactly = 0) { pantries.deletePantryMember(any<PantryId>(), any<UserId>()) }
        verify(exactly = 0) { defaultPantryReassigner.reassignIfNeeded(any(), any()) }
    }

    @Test
    fun `propagates denial when caller is not the owner`() {
        every { guard.checkAccess(pantryId, ownerId, true) } throws
            PantryActionDeniedException(ownerId, pantryId)

        shouldThrow<PantryActionDeniedException> {
            useCase.kickPantryMember(pantryId, ownerId, memberId)
        }

        verify(exactly = 0) { pantries.deletePantryMember(any<PantryId>(), any<UserId>()) }
    }

    private fun member(userId: UserId, role: PantryRole) =
        PantryMember(pantryId, userId, role, Instant.now())
}
