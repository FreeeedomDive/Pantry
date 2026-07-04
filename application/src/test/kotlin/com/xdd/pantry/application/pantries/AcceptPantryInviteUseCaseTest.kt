package com.xdd.pantry.application.pantries

import com.xdd.pantry.application.users.RegisterUserUseCase
import com.xdd.pantry.domain.pantries.Pantry
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryInvite
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.TelegramUserId
import com.xdd.pantry.domain.users.User
import com.xdd.pantry.domain.users.UserId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class AcceptPantryInviteUseCaseTest {

    private val invites = mockk<PantryInviteRepository>()
    private val pantries = mockk<PantryRepository>()
    private val registerUser = mockk<RegisterUserUseCase>()
    private val useCase = AcceptPantryInviteUseCase(invites, pantries, registerUser)

    private val pantryId = PantryId(UUID.randomUUID())
    private val pantry = Pantry(pantryId, "Дом", Instant.now())
    private val token = UUID.randomUUID()
    private val ownerId = UserId(UUID.randomUUID())
    private val telegramUserId = TelegramUserId(1234567)
    private val invitee = User(UserId(UUID.randomUUID()), telegramUserId, Instant.now())

    @Test
    fun `joins the pantry by a valid invite`() {
        every { invites.find(token) } returns invite(expiresAt = Instant.now().plusSeconds(3600))
        every { pantries.getPantry(pantryId) } returns pantry
        every { registerUser.findOrRegister(telegramUserId) } returns invitee
        every { pantries.getPantryMember(pantryId, invitee.id) } returns null
        every { pantries.save(any<PantryMember>()) } answers { firstArg() }

        val result = useCase.acceptInvite(telegramUserId, token)

        result.shouldBeInstanceOf<AcceptInviteResult.Joined>().pantry shouldBe pantry
        verify(exactly = 1) {
            pantries.save(match<PantryMember> { it.userId == invitee.id && it.role == PantryRole.MEMBER })
        }
    }

    @Test
    fun `reports an existing membership without saving`() {
        every { invites.find(token) } returns invite(expiresAt = Instant.now().plusSeconds(3600))
        every { pantries.getPantry(pantryId) } returns pantry
        every { registerUser.findOrRegister(telegramUserId) } returns invitee
        every { pantries.getPantryMember(pantryId, invitee.id) } returns
            PantryMember(pantryId, invitee.id, PantryRole.MEMBER, Instant.now())

        val result = useCase.acceptInvite(telegramUserId, token)

        result.shouldBeInstanceOf<AcceptInviteResult.AlreadyMember>().pantry shouldBe pantry
        verify(exactly = 0) { pantries.save(any<PantryMember>()) }
    }

    @Test
    fun `rejects an expired invite`() {
        every { invites.find(token) } returns invite(expiresAt = Instant.now().minusSeconds(60))

        val result = useCase.acceptInvite(telegramUserId, token)

        result shouldBe AcceptInviteResult.InvalidInvite
        verify(exactly = 0) { pantries.save(any<PantryMember>()) }
    }

    @Test
    fun `rejects an unknown token`() {
        every { invites.find(token) } returns null

        val result = useCase.acceptInvite(telegramUserId, token)

        result shouldBe AcceptInviteResult.InvalidInvite
    }

    private fun invite(expiresAt: Instant) =
        PantryInvite(token, pantryId, ownerId, Instant.now().minusSeconds(600), expiresAt)
}
