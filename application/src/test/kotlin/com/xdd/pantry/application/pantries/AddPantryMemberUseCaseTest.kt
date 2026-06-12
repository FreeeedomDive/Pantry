package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.pantries.PantryActionDeniedException
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class AddPantryMemberUseCaseTest {

    private val pantries = mockk<PantryRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val useCase = AddPantryMemberUseCase(pantries, guard)

    private val pantryId = PantryId(UUID.randomUUID())
    private val ownerId = UserId(UUID.randomUUID())
    private val newMemberId = UserId(UUID.randomUUID())

    @Test
    fun `adds new member with MEMBER role`() {
        every { guard.checkAccess(pantryId, ownerId, true) } returns owner()
        every { pantries.getPantryMember(pantryId, newMemberId) } returns null
        every { pantries.save(any<PantryMember>()) } answers { firstArg() }

        useCase.addPantryMember(pantryId, ownerId, newMemberId)

        verify(exactly = 1) {
            pantries.save(match<PantryMember> {
                it.pantryId == pantryId && it.userId == newMemberId && it.role == PantryRole.MEMBER
            })
        }
    }

    @Test
    fun `does nothing when user is already a member`() {
        every { guard.checkAccess(pantryId, ownerId, true) } returns owner()
        every { pantries.getPantryMember(pantryId, newMemberId) } returns
            PantryMember(pantryId, newMemberId, PantryRole.MEMBER, Instant.now())

        useCase.addPantryMember(pantryId, ownerId, newMemberId)

        verify(exactly = 0) { pantries.save(any<PantryMember>()) }
    }

    @Test
    fun `propagates denial when caller is not the owner`() {
        every { guard.checkAccess(pantryId, ownerId, true) } throws
            PantryActionDeniedException(ownerId, pantryId)

        shouldThrow<PantryActionDeniedException> {
            useCase.addPantryMember(pantryId, ownerId, newMemberId)
        }

        verify(exactly = 0) { pantries.save(any<PantryMember>()) }
    }

    private fun owner() = PantryMember(pantryId, ownerId, PantryRole.OWNER, Instant.now())
}
