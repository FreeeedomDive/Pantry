package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.pantries.PantryActionDeniedException
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryInvite
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID

class CreatePantryInviteUseCaseTest {

    private val invites = mockk<PantryInviteRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val useCase = CreatePantryInviteUseCase(invites, guard)

    private val pantryId = PantryId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())

    @Test
    fun `creates an invite with a week-long expiry`() {
        every { guard.checkAccess(pantryId, userId, shouldBeOwner = true) } returns owner()
        every { invites.save(any()) } answers { firstArg() }

        val invite = useCase.createInvite(userId, pantryId)

        invite.pantryId shouldBe pantryId
        invite.createdBy shouldBe userId
        Duration.between(invite.createdAt, invite.expiresAt) shouldBe Duration.ofDays(7)
        verify(exactly = 1) { invites.save(any<PantryInvite>()) }
    }

    @Test
    fun `denies invite creation for a non-owner`() {
        every { guard.checkAccess(pantryId, userId, shouldBeOwner = true) } throws
            PantryActionDeniedException(userId, pantryId)

        shouldThrow<PantryActionDeniedException> { useCase.createInvite(userId, pantryId) }

        verify(exactly = 0) { invites.save(any()) }
    }

    private fun owner() = PantryMember(pantryId, userId, PantryRole.OWNER, Instant.now())
}
