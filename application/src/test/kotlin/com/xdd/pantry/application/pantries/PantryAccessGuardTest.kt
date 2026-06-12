package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.pantries.PantryActionDeniedException
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class PantryAccessGuardTest {

    private val pantries = mockk<PantryRepository>()
    private val guard = PantryAccessGuard(pantries)

    private val pantryId = PantryId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())

    @Test
    fun `returns member when user belongs to pantry`() {
        val member = member(PantryRole.MEMBER)
        every { pantries.getPantryMember(pantryId, userId) } returns member

        guard.checkAccess(pantryId, userId) shouldBe member
    }

    @Test
    fun `denies access for non-member`() {
        every { pantries.getPantryMember(pantryId, userId) } returns null

        shouldThrow<PantryActionDeniedException> { guard.checkAccess(pantryId, userId) }
    }

    @Test
    fun `denies owner-only action for plain member`() {
        every { pantries.getPantryMember(pantryId, userId) } returns member(PantryRole.MEMBER)

        shouldThrow<PantryActionDeniedException> {
            guard.checkAccess(pantryId, userId, shouldBeOwner = true)
        }
    }

    @Test
    fun `allows owner-only action for owner`() {
        val owner = member(PantryRole.OWNER)
        every { pantries.getPantryMember(pantryId, userId) } returns owner

        guard.checkAccess(pantryId, userId, shouldBeOwner = true) shouldBe owner
    }

    private fun member(role: PantryRole) = PantryMember(pantryId, userId, role, Instant.now())
}
