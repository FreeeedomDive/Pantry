package com.xdd.pantry.application.receipts

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.receipts.Confidence
import com.xdd.pantry.domain.receipts.DraftStatus
import com.xdd.pantry.domain.receipts.ReceiptDraft
import com.xdd.pantry.domain.receipts.RecognizedAction
import com.xdd.pantry.domain.receipts.RecognizedLine
import com.xdd.pantry.domain.receipts.RecognizedReceipt
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.UUID

class CreateReceiptDraftUseCaseTest {

    private val drafts = mockk<ReceiptDraftRepository>()
    private val useCase = CreateReceiptDraftUseCase(drafts)

    private val pantryId = PantryId(UUID.randomUUID())

    @Test
    fun `creates a READY draft from the recognized receipt`() {
        val milkId = ProductId(UUID.randomUUID())
        every { drafts.save(any()) } answers { firstArg() }

        val draft = useCase.createDraft(
            pantryId,
            RecognizedReceipt(
                listOf(
                    RecognizedLine("МОЛОКО 3.2", RecognizedAction.MATCH, milkId, null, null, 1, Confidence.HIGH),
                    RecognizedLine("ХЛЕБ", RecognizedAction.CREATE, null, "Хлеб", null, 2, Confidence.LOW),
                ),
            ),
        )

        draft.status shouldBe DraftStatus.READY
        draft.pantryId shouldBe pantryId
        draft.lines shouldHaveSize 2
        verify(exactly = 1) {
            drafts.save(match<ReceiptDraft> { it.status == DraftStatus.READY && it.lines.size == 2 })
        }
    }
}
