package com.xdd.pantry.application.pantries

import com.xdd.pantry.application.users.UserRepository
import com.xdd.pantry.domain.pantries.PantryActionDeniedException
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.TelegramUserId
import com.xdd.pantry.domain.users.User
import com.xdd.pantry.domain.users.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class GetPantryMembersUseCaseTest {

    private val pantries = mockk<PantryRepository>()
    private val users = mockk<UserRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val useCase = GetPantryMembersUseCase(pantries, users, guard)

    private val pantryId = PantryId(UUID.randomUUID())
    private val owner = User(UserId(UUID.randomUUID()), TelegramUserId(1L), Instant.now())
    private val member = User(UserId(UUID.randomUUID()), TelegramUserId(2L), Instant.now())

    @Test
    fun `returns members with telegram ids, owner first`() {
        val ownerJoined = Instant.parse("2026-01-01T00:00:00Z")
        val memberJoined = Instant.parse("2026-02-01T00:00:00Z")
        every { guard.checkAccess(pantryId, member.id) } returns
            PantryMember(pantryId, member.id, PantryRole.MEMBER, memberJoined)
        every { pantries.getPantryMembers(pantryId) } returns listOf(
            PantryMember(pantryId, member.id, PantryRole.MEMBER, memberJoined),
            PantryMember(pantryId, owner.id, PantryRole.OWNER, ownerJoined),
        )
        every { users.findByIds(any()) } returns listOf(owner, member)

        val members = useCase.getPantryMembers(member.id, pantryId)

        members shouldBe listOf(
            PantryMemberInfo(owner.telegramUserId, PantryRole.OWNER, ownerJoined),
            PantryMemberInfo(member.telegramUserId, PantryRole.MEMBER, memberJoined),
        )
    }

    @Test
    fun `denies listing for a non-member`() {
        val strangerId = UserId(UUID.randomUUID())
        every { guard.checkAccess(pantryId, strangerId) } throws
            PantryActionDeniedException(strangerId, pantryId)

        shouldThrow<PantryActionDeniedException> { useCase.getPantryMembers(strangerId, pantryId) }
    }
}
