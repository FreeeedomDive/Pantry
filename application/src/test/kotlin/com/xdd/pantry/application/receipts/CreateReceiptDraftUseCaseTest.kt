package com.xdd.pantry.application.receipts

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.receipts.DraftStatus
import com.xdd.pantry.domain.receipts.ExtractedLine
import com.xdd.pantry.domain.receipts.ExtractedReceipt
import com.xdd.pantry.domain.receipts.ReceiptDraft
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
    fun `creates an EXTRACTED draft from the extracted receipt`() {
        every { drafts.save(any()) } answers { firstArg() }

        val draft = useCase.createFromExtracted(
            pantryId,
            ExtractedReceipt(listOf(ExtractedLine("МОЛОКО 3.2", 1), ExtractedLine("ХЛЕБ", 2))),
        )

        draft.status shouldBe DraftStatus.EXTRACTED
        draft.pantryId shouldBe pantryId
        draft.lines shouldHaveSize 2
        draft.lines.map { it.rawText } shouldBe listOf("МОЛОКО 3.2", "ХЛЕБ")
        verify(exactly = 1) {
            drafts.save(match<ReceiptDraft> { it.status == DraftStatus.EXTRACTED && it.lines.size == 2 })
        }
    }
}
