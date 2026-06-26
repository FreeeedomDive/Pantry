package com.xdd.pantry.application.receipts

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.receipts.Confidence
import com.xdd.pantry.domain.receipts.DraftStatus
import com.xdd.pantry.domain.receipts.ReceiptDraft
import com.xdd.pantry.domain.receipts.ReceiptImage
import com.xdd.pantry.domain.receipts.RecognizedAction
import com.xdd.pantry.domain.receipts.RecognizedLine
import com.xdd.pantry.domain.receipts.RecognizedReceipt
import com.xdd.pantry.domain.users.UserId
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.UUID

class CreateReceiptDraftUseCaseTest {

    private val recognize = mockk<RecognizeReceiptUseCase>()
    private val drafts = mockk<ReceiptDraftRepository>()
    private val useCase = CreateReceiptDraftUseCase(recognize, drafts)

    private val pantryId = PantryId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())
    private val images = listOf(ReceiptImage("image/jpeg", byteArrayOf(1, 2, 3)))

    @Test
    fun `creates a READY draft from the recognized receipt`() = runTest {
        val milkId = ProductId(UUID.randomUUID())
        coEvery { recognize.recognizeReceipt(userId, pantryId, images) } returns RecognizedReceipt(
            listOf(
                RecognizedLine("МОЛОКО 3.2", RecognizedAction.MATCH, milkId, null, null, 1, Confidence.HIGH),
                RecognizedLine("ХЛЕБ", RecognizedAction.CREATE, null, "Хлеб", null, 2, Confidence.LOW),
            ),
        )
        every { drafts.save(any()) } answers { firstArg() }

        val draft = useCase.createDraft(userId, pantryId, images)

        draft.status shouldBe DraftStatus.READY
        draft.pantryId shouldBe pantryId
        draft.lines shouldHaveSize 2
        draft.lines.first { it.action == RecognizedAction.MATCH }.productId shouldBe milkId
        draft.lines.first { it.action == RecognizedAction.CREATE }.proposedName shouldBe "Хлеб"
        verify(exactly = 1) {
            drafts.save(match<ReceiptDraft> { it.status == DraftStatus.READY && it.lines.size == 2 })
        }
    }
}
