package com.xdd.pantry.application.receipts

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.application.products.ProductAliasRepository
import com.xdd.pantry.application.products.ProductRepository
import com.xdd.pantry.domain.pantries.PantryActionDeniedException
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.products.ProductAlias
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.receipts.Confidence
import com.xdd.pantry.domain.receipts.ExtractedLine
import com.xdd.pantry.domain.receipts.ExtractedReceipt
import com.xdd.pantry.domain.receipts.ReceiptImage
import com.xdd.pantry.domain.receipts.RecognizedAction
import com.xdd.pantry.domain.receipts.RecognizedLine
import com.xdd.pantry.domain.receipts.RecognizedReceipt
import com.xdd.pantry.domain.users.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class RecognizeReceiptUseCaseTest {

    private val extractor = mockk<ReceiptExtractor>()
    private val recognizer = mockk<ReceiptRecognizer>()
    private val products = mockk<ProductRepository>(relaxed = true)
    private val aliases = mockk<ProductAliasRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val useCase = RecognizeReceiptUseCase(extractor, recognizer, products, aliases, guard)

    private val pantryId = PantryId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())
    private val images = listOf(ReceiptImage("image/jpeg", byteArrayOf(1, 2, 3)))

    @Test
    fun `matches lines locally via aliases without calling the recognizer`() = runTest {
        val milkId = ProductId(UUID.randomUUID())
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        coEvery { extractor.extract(images) } returns ExtractedReceipt(listOf(ExtractedLine("Молоко 3.2", 1)))
        every { aliases.getPantryAliases(pantryId) } returns listOf(ProductAlias(pantryId, "МОЛОКО 3.2", milkId))

        val result = useCase.recognizeReceipt(userId, pantryId, images)

        result.lines shouldHaveSize 1
        result.lines.single().action shouldBe RecognizedAction.MATCH
        result.lines.single().productId shouldBe milkId
        result.lines.single().confidence shouldBe Confidence.HIGH
        coVerify(exactly = 0) { recognizer.recognize(any(), any()) }
    }

    @Test
    fun `sends only alias-miss lines to the recognizer`() = runTest {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        val knownId = ProductId(UUID.randomUUID())
        coEvery { extractor.extract(images) } returns ExtractedReceipt(
            listOf(ExtractedLine("МОЛОКО 3.2", 1), ExtractedLine("НЕЧТО НОВОЕ", 2)),
        )
        every { aliases.getPantryAliases(pantryId) } returns listOf(ProductAlias(pantryId, "МОЛОКО 3.2", knownId))
        coEvery { recognizer.recognize(any(), any()) } returns RecognizedReceipt(
            listOf(RecognizedLine("НЕЧТО НОВОЕ", RecognizedAction.CREATE, null, "Нечто новое", null, 2, Confidence.LOW)),
        )

        val result = useCase.recognizeReceipt(userId, pantryId, images)

        result.lines shouldHaveSize 2
        result.lines.first { it.action == RecognizedAction.MATCH }.productId shouldBe knownId
        result.lines.first { it.action == RecognizedAction.CREATE }.proposedName shouldBe "Нечто новое"
        coVerify(exactly = 1) { recognizer.recognize(match { it.size == 1 && it.single().rawText == "НЕЧТО НОВОЕ" }, any()) }
    }

    @Test
    fun `does not call the recognizer when extraction is empty`() = runTest {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        coEvery { extractor.extract(images) } returns ExtractedReceipt(emptyList())

        useCase.recognizeReceipt(userId, pantryId, images).lines shouldHaveSize 0

        coVerify(exactly = 0) { recognizer.recognize(any(), any()) }
    }

    @Test
    fun `denies and does not extract when user is not a member`() = runTest {
        every { guard.checkAccess(pantryId, userId, false) } throws PantryActionDeniedException(userId, pantryId)

        shouldThrow<PantryActionDeniedException> {
            useCase.recognizeReceipt(userId, pantryId, images)
        }

        coVerify(exactly = 0) { extractor.extract(any()) }
    }

    private fun member() = PantryMember(pantryId, userId, PantryRole.MEMBER, Instant.now())
}
