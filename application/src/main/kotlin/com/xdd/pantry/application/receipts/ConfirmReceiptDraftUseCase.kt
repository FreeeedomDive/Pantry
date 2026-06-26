package com.xdd.pantry.application.receipts

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.application.products.ProductAliasRepository
import com.xdd.pantry.application.products.ProductRepository
import com.xdd.pantry.application.products.StockRepository
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.products.ProductAlias
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.products.Quantity
import com.xdd.pantry.domain.products.StockItem
import com.xdd.pantry.domain.products.StockItemId
import com.xdd.pantry.domain.products.normalizeAlias
import com.xdd.pantry.domain.receipts.DraftId
import com.xdd.pantry.domain.receipts.DraftStatus
import com.xdd.pantry.domain.receipts.ReceiptDraftNotFoundException
import com.xdd.pantry.domain.receipts.ReceiptDraftNotReadyException
import com.xdd.pantry.domain.receipts.RecognizedAction
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class ConfirmReceiptDraftUseCase(
    private val drafts: ReceiptDraftRepository,
    private val products: ProductRepository,
    private val stock: StockRepository,
    private val aliases: ProductAliasRepository,
    private val accessGuard: PantryAccessGuard,
) {
    fun confirmDraft(userId: UserId, pantryId: PantryId, draftId: DraftId) {
        accessGuard.checkAccess(pantryId, userId)
        val draft = drafts.getDraft(draftId) ?: throw ReceiptDraftNotFoundException(draftId)
        if (draft.pantryId != pantryId) throw ReceiptDraftNotFoundException(draftId)
        if (draft.status != DraftStatus.READY) throw ReceiptDraftNotReadyException(draftId, draft.status)

        val knownAliases = aliases.getPantryAliases(pantryId).mapTo(mutableSetOf()) { it.alias }

        draft.lines.forEach { line ->
            val productId: ProductId = when (line.action) {
                RecognizedAction.MATCH -> line.productId ?: return@forEach
                RecognizedAction.CREATE -> {
                    val name = line.proposedName ?: return@forEach
                    products.save(Product(ProductId(UUID.randomUUID()), pantryId, name, line.proposedBrand)).id
                }
                RecognizedAction.UNSURE -> return@forEach
            }

            if (line.quantity > 0) {
                stock.save(StockItem(StockItemId(UUID.randomUUID()), productId, Quantity(line.quantity), Instant.now(), null))
            }

            val alias = normalizeAlias(line.rawText)
            if (knownAliases.add(alias)) {
                aliases.save(ProductAlias(pantryId, alias, productId))
            }
        }

        drafts.updateStatus(draftId, DraftStatus.CONFIRMED)
    }
}
