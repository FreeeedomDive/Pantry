package com.xdd.pantry.application.receipts

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.application.products.ProductAliasRepository
import com.xdd.pantry.application.products.ProductRepository
import com.xdd.pantry.application.products.StockRepository
import com.xdd.pantry.domain.pantries.PantryActionDeniedException
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.products.ProductAlias
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.products.Quantity
import com.xdd.pantry.domain.products.StockItem
import com.xdd.pantry.domain.receipts.Confidence
import com.xdd.pantry.domain.receipts.DraftId
import com.xdd.pantry.domain.receipts.DraftLine
import com.xdd.pantry.domain.receipts.DraftLineId
import com.xdd.pantry.domain.receipts.DraftStatus
import com.xdd.pantry.domain.receipts.ReceiptDraft
import com.xdd.pantry.domain.receipts.ReceiptDraftNotReadyException
import com.xdd.pantry.domain.receipts.RecognizedAction
import com.xdd.pantry.domain.users.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class ConfirmReceiptDraftUseCaseTest {

    private val drafts = mockk<ReceiptDraftRepository>()
    private val products = mockk<ProductRepository>()
    private val stock = mockk<StockRepository>()
    private val aliases = mockk<ProductAliasRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val useCase = ConfirmReceiptDraftUseCase(drafts, products, stock, aliases, guard)

    private val pantryId = PantryId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())
    private val draftId = DraftId(UUID.randomUUID())
    private val milkId = ProductId(UUID.randomUUID())
    private val milkExpiry = LocalDate.of(2026, 7, 20)

    @Test
    fun `applies lines to stock, learns aliases and confirms the draft`() {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        every { drafts.getDraft(draftId) } returns draft(
            line("МОЛОКО 3.2", RecognizedAction.MATCH, productId = milkId, quantity = 1, expiresAt = milkExpiry),
            line("ХЛЕБ", RecognizedAction.CREATE, proposedName = "Хлеб", quantity = 2),
            line("ПАКЕТ", RecognizedAction.UNSURE, quantity = 1),
        )
        every { aliases.getPantryAliases(pantryId) } returns emptyList()
        every { products.save(any()) } answers { firstArg() }
        every { stock.save(any()) } answers { firstArg() }
        justRun { aliases.save(any()) }
        justRun { drafts.updateStatus(any(), any()) }

        useCase.confirmDraft(userId, pantryId, draftId)

        verify(exactly = 2) { stock.save(any<StockItem>()) }
        verify(exactly = 1) {
            stock.save(match<StockItem> { it.productId == milkId && it.quantity == Quantity(1) && it.expiresAt == milkExpiry })
        }
        verify(exactly = 1) { products.save(match<Product> { it.name == "Хлеб" && it.pantryId == pantryId }) }
        verify(exactly = 2) { aliases.save(any<ProductAlias>()) }
        verify(exactly = 1) { drafts.updateStatus(draftId, DraftStatus.CONFIRMED) }
    }

    @Test
    fun `does not re-save an alias that already exists`() {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        every { drafts.getDraft(draftId) } returns draft(
            line("МОЛОКО 3.2", RecognizedAction.MATCH, productId = milkId, quantity = 1),
        )
        every { aliases.getPantryAliases(pantryId) } returns listOf(ProductAlias(pantryId, "МОЛОКО 3.2", milkId))
        every { stock.save(any()) } answers { firstArg() }
        justRun { drafts.updateStatus(any(), any()) }

        useCase.confirmDraft(userId, pantryId, draftId)

        verify(exactly = 1) { stock.save(any<StockItem>()) }
        verify(exactly = 0) { aliases.save(any()) }
    }

    @Test
    fun `rejects a draft that is not READY`() {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        every { drafts.getDraft(draftId) } returns
            draft().copy(status = DraftStatus.CONFIRMED)

        shouldThrow<ReceiptDraftNotReadyException> { useCase.confirmDraft(userId, pantryId, draftId) }

        verify(exactly = 0) { stock.save(any()) }
        verify(exactly = 0) { drafts.updateStatus(any(), any()) }
    }

    @Test
    fun `denies confirmation when user is not a member`() {
        every { guard.checkAccess(pantryId, userId, false) } throws PantryActionDeniedException(userId, pantryId)

        shouldThrow<PantryActionDeniedException> { useCase.confirmDraft(userId, pantryId, draftId) }

        verify(exactly = 0) { drafts.getDraft(any()) }
        verify(exactly = 0) { stock.save(any()) }
    }

    private fun member() = PantryMember(pantryId, userId, PantryRole.MEMBER, Instant.now())

    private fun draft(vararg lines: DraftLine) =
        ReceiptDraft(draftId, pantryId, DraftStatus.READY, Instant.now(), lines.toList())

    private fun line(
        rawText: String,
        action: RecognizedAction,
        productId: ProductId? = null,
        proposedName: String? = null,
        quantity: Int = 1,
        expiresAt: LocalDate? = null,
    ) = DraftLine(DraftLineId(UUID.randomUUID()), rawText, action, productId, proposedName, null, quantity, Confidence.HIGH, expiresAt)
}
