package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.pantries.CannotDeleteLastPantryException
import com.xdd.pantry.domain.pantries.Pantry
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

class LeaveOrDeletePantryUseCaseTest {

    private val pantries = mockk<PantryRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val defaultPantryReassigner = mockk<DefaultPantryReassigner>()
    private val useCase = LeaveOrDeletePantryUseCase(pantries, guard, defaultPantryReassigner)

    private val userId = UserId(UUID.randomUUID())
    private val pantryId = PantryId(UUID.randomUUID())

    @Test
    fun `owner deletes the pantry when they have other pantries left`() {
        every { guard.checkAccess(pantryId, userId) } returns member(PantryRole.OWNER)
        every { pantries.getUserMemberships(userId) } returns listOf(somePantryMember(userId), somePantryMember(userId))
        justRun { pantries.delete(pantryId) }

        useCase.leaveOrDelete(userId, pantryId)

        verify(exactly = 1) { pantries.delete(pantryId) }
        verify(exactly = 0) { pantries.deletePantryMember(any(), any()) }
    }

    @Test
    fun `owner cannot delete their last remaining pantry`() {
        every { guard.checkAccess(pantryId, userId) } returns member(PantryRole.OWNER)
        every { pantries.getUserMemberships(userId) } returns listOf(somePantryMember(userId))

        shouldThrow<CannotDeleteLastPantryException> { useCase.leaveOrDelete(userId, pantryId) }

        verify(exactly = 0) { pantries.delete(any()) }
    }

    @Test
    fun `member leaves the pantry and gets their default reassigned`() {
        every { guard.checkAccess(pantryId, userId) } returns member(PantryRole.MEMBER)
        justRun { pantries.deletePantryMember(pantryId, userId) }
        justRun { defaultPantryReassigner.reassignIfNeeded(userId, pantryId) }

        useCase.leaveOrDelete(userId, pantryId)

        verify(exactly = 1) { pantries.deletePantryMember(pantryId, userId) }
        verify(exactly = 1) { defaultPantryReassigner.reassignIfNeeded(userId, pantryId) }
        verify(exactly = 0) { pantries.delete(any()) }
    }

    @Test
    fun `denies action for a non-member`() {
        every { guard.checkAccess(pantryId, userId) } throws PantryActionDeniedException(userId, pantryId)

        shouldThrow<PantryActionDeniedException> { useCase.leaveOrDelete(userId, pantryId) }

        verify(exactly = 0) { pantries.delete(any()) }
        verify(exactly = 0) { pantries.deletePantryMember(any(), any()) }
    }

    private fun member(role: PantryRole) = PantryMember(pantryId, userId, role, Instant.now())
    private fun somePantryMember(userId: UserId) = PantryMember(PantryId(UUID.randomUUID()), userId, PantryRole.OWNER, Instant.now())
}
