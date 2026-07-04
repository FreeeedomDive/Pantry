package com.xdd.pantry.application.receipts

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.application.users.UserRepository
import com.xdd.pantry.domain.pantries.PantryActionDeniedException
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.receipts.DraftId
import com.xdd.pantry.domain.receipts.DraftStatus
import com.xdd.pantry.domain.receipts.ReceiptDraft
import com.xdd.pantry.domain.receipts.ReceiptDraftNotReadyException
import com.xdd.pantry.domain.users.TelegramUserId
import com.xdd.pantry.domain.users.User
import com.xdd.pantry.domain.users.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class MoveReceiptDraftUseCaseTest {

    private val drafts = mockk<ReceiptDraftRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val users = mockk<UserRepository>()
    private val queue = mockk<ReceiptMatchingQueue>()
    private val useCase = MoveReceiptDraftUseCase(drafts, guard, users, queue)

    private val userId = UserId(UUID.randomUUID())
    private val telegramUserId = TelegramUserId(1234567)
    private val currentPantryId = PantryId(UUID.randomUUID())
    private val targetPantryId = PantryId(UUID.randomUUID())
    private val draftId = DraftId(UUID.randomUUID())

    @Test
    fun `moves a READY draft and requests re-matching against the target pantry`() {
        every { guard.checkAccess(currentPantryId, userId) } returns member(currentPantryId)
        every { guard.checkAccess(targetPantryId, userId) } returns member(targetPantryId)
        every { drafts.getDraft(draftId) } returns readyDraft()
        every { users.findById(userId) } returns User(userId, telegramUserId, Instant.now())
        justRun { drafts.updateStatus(draftId, DraftStatus.MATCHING) }
        justRun { queue.requestMatching(draftId, targetPantryId, telegramUserId, telegramUserId.value) }

        useCase.move(userId, currentPantryId, draftId, targetPantryId)

        verify(exactly = 1) { drafts.updateStatus(draftId, DraftStatus.MATCHING) }
        verify(exactly = 1) { queue.requestMatching(draftId, targetPantryId, telegramUserId, telegramUserId.value) }
    }

    @Test
    fun `rejects moving a draft that is not READY`() {
        every { guard.checkAccess(currentPantryId, userId) } returns member(currentPantryId)
        every { guard.checkAccess(targetPantryId, userId) } returns member(targetPantryId)
        every { drafts.getDraft(draftId) } returns readyDraft().copy(status = DraftStatus.CONFIRMED)

        shouldThrow<ReceiptDraftNotReadyException> {
            useCase.move(userId, currentPantryId, draftId, targetPantryId)
        }

        verify(exactly = 0) { queue.requestMatching(any(), any(), any(), any()) }
    }

    @Test
    fun `denies moving into a pantry the user is not a member of`() {
        every { guard.checkAccess(currentPantryId, userId) } returns member(currentPantryId)
        every { guard.checkAccess(targetPantryId, userId) } throws PantryActionDeniedException(userId, targetPantryId)

        shouldThrow<PantryActionDeniedException> {
            useCase.move(userId, currentPantryId, draftId, targetPantryId)
        }

        verify(exactly = 0) { drafts.updateStatus(any(), any()) }
        verify(exactly = 0) { queue.requestMatching(any(), any(), any(), any()) }
    }

    private fun readyDraft() =
        ReceiptDraft(draftId, currentPantryId, DraftStatus.READY, Instant.now(), emptyList())

    private fun member(pantryId: PantryId) = PantryMember(pantryId, userId, PantryRole.MEMBER, Instant.now())
}
