package com.xdd.pantry.application.receipts

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.domain.pantries.PantryActionDeniedException
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.receipts.Confidence
import com.xdd.pantry.domain.receipts.DraftId
import com.xdd.pantry.domain.receipts.DraftLine
import com.xdd.pantry.domain.receipts.DraftStatus
import com.xdd.pantry.domain.receipts.ReceiptDraft
import com.xdd.pantry.domain.receipts.ReceiptDraftNotReadyException
import com.xdd.pantry.domain.receipts.RecognizedAction
import com.xdd.pantry.domain.receipts.RecognizedLine
import com.xdd.pantry.domain.receipts.RecognizedReceipt
import com.xdd.pantry.domain.users.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class UpdateReceiptDraftUseCaseTest {

    private val drafts = mockk<ReceiptDraftRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val useCase = UpdateReceiptDraftUseCase(drafts, guard)

    private val pantryId = PantryId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())
    private val draftId = DraftId(UUID.randomUUID())

    @Test
    fun `replaces the lines of a READY draft`() {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        every { drafts.getDraft(draftId) } returns readyDraft()
        val newLines = slot<List<DraftLine>>()
        every { drafts.replaceLines(draftId, capture(newLines)) } answers { readyDraft() }

        val expiry = LocalDate.of(2026, 7, 20)
        val edited = RecognizedReceipt(
            listOf(RecognizedLine("Молоко", RecognizedAction.CREATE, null, "Молоко", null, 3, Confidence.HIGH, expiry)),
        )
        useCase.updateDraft(userId, pantryId, draftId, edited)

        newLines.captured shouldHaveSize 1
        newLines.captured.single().proposedName shouldBe "Молоко"
        newLines.captured.single().quantity shouldBe 3
        newLines.captured.single().expiresAt shouldBe expiry
    }

    @Test
    fun `rejects update of a draft that is not READY`() {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        every { drafts.getDraft(draftId) } returns
            ReceiptDraft(draftId, pantryId, DraftStatus.CONFIRMED, Instant.now(), emptyList())

        shouldThrow<ReceiptDraftNotReadyException> {
            useCase.updateDraft(userId, pantryId, draftId, RecognizedReceipt(emptyList()))
        }

        verify(exactly = 0) { drafts.replaceLines(any(), any()) }
    }

    @Test
    fun `denies update when user is not a member`() {
        every { guard.checkAccess(pantryId, userId, false) } throws PantryActionDeniedException(userId, pantryId)

        shouldThrow<PantryActionDeniedException> {
            useCase.updateDraft(userId, pantryId, draftId, RecognizedReceipt(emptyList()))
        }

        verify(exactly = 0) { drafts.getDraft(any()) }
        verify(exactly = 0) { drafts.replaceLines(any(), any()) }
    }

    private fun readyDraft() = ReceiptDraft(draftId, pantryId, DraftStatus.READY, Instant.now(), emptyList())

    private fun member() = PantryMember(pantryId, userId, PantryRole.MEMBER, Instant.now())
}
