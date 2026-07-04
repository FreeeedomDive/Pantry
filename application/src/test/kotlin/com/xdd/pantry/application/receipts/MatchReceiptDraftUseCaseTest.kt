package com.xdd.pantry.application.receipts

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.application.products.ProductAliasRepository
import com.xdd.pantry.application.products.ProductRepository
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.products.ProductAlias
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.receipts.Confidence
import com.xdd.pantry.domain.receipts.DraftId
import com.xdd.pantry.domain.receipts.DraftLine
import com.xdd.pantry.domain.receipts.DraftLineId
import com.xdd.pantry.domain.receipts.DraftStatus
import com.xdd.pantry.domain.receipts.ReceiptDraft
import com.xdd.pantry.domain.receipts.RecognizedAction
import com.xdd.pantry.domain.users.UserId
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class MatchReceiptDraftUseCaseTest {

    private val recognizer = mockk<ReceiptRecognizer>()
    private val products = mockk<ProductRepository>()
    private val aliases = mockk<ProductAliasRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val recognizeReceipt = RecognizeReceiptUseCase(recognizer, products, aliases, guard)

    private val drafts = mockk<ReceiptDraftRepository>()
    private val useCase = MatchReceiptDraftUseCase(recognizeReceipt, drafts)

    private val userId = UserId(UUID.randomUUID())
    private val pantryId = PantryId(UUID.randomUUID())
    private val draftId = DraftId(UUID.randomUUID())
    private val milkId = ProductId(UUID.randomUUID())

    @Test
    fun `matches the extracted draft against the pantry catalog by alias`() = runBlocking {
        val extractedDraft = ReceiptDraft(
            draftId, pantryId, DraftStatus.EXTRACTED, Instant.now(),
            listOf(rawLine("МОЛОКО 3.2", quantity = 2)),
        )
        every { drafts.getDraft(draftId) } returns extractedDraft
        every { guard.checkAccess(pantryId, userId) } returns member()
        every { aliases.getPantryAliases(pantryId) } returns listOf(ProductAlias(pantryId, "МОЛОКО 3.2", milkId))
        every { products.getPantryProducts(pantryId) } returns emptyList()
        val applied = slot<List<DraftLine>>()
        every { drafts.applyMatch(draftId, pantryId, capture(applied)) } answers { extractedDraft }

        useCase.match(userId, pantryId, draftId)

        applied.captured.single().action shouldBe RecognizedAction.MATCH
        applied.captured.single().productId shouldBe milkId
        applied.captured.single().quantity shouldBe 2
        verify(exactly = 1) { drafts.applyMatch(draftId, pantryId, any()) }
    }

    @Test
    fun `sends unknown lines to the recognizer`() = runBlocking {
        val extractedDraft = ReceiptDraft(
            draftId, pantryId, DraftStatus.EXTRACTED, Instant.now(),
            listOf(rawLine("НЕЧТО НОВОЕ", quantity = 1)),
        )
        every { drafts.getDraft(draftId) } returns extractedDraft
        every { guard.checkAccess(pantryId, userId) } returns member()
        every { aliases.getPantryAliases(pantryId) } returns emptyList()
        every { products.getPantryProducts(pantryId) } returns emptyList()
        coEvery { recognizer.recognize(any(), any()) } returns com.xdd.pantry.domain.receipts.RecognizedReceipt(
            listOf(
                com.xdd.pantry.domain.receipts.RecognizedLine(
                    "НЕЧТО НОВОЕ", RecognizedAction.CREATE, null, "Нечто новое", null, 1, Confidence.LOW,
                ),
            ),
        )
        every { drafts.applyMatch(draftId, pantryId, any()) } answers { extractedDraft }

        useCase.match(userId, pantryId, draftId)

        verify(exactly = 1) { drafts.applyMatch(draftId, pantryId, any()) }
    }

    private fun member() = PantryMember(pantryId, userId, PantryRole.MEMBER, Instant.now())

    private fun rawLine(rawText: String, quantity: Int) = DraftLine(
        DraftLineId(UUID.randomUUID()), rawText, RecognizedAction.UNSURE, null, null, null, quantity, Confidence.LOW,
    )
}
