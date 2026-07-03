package com.xdd.pantry.application.receipts

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.receipts.DraftId
import com.xdd.pantry.domain.receipts.DraftLine
import com.xdd.pantry.domain.receipts.DraftLineId
import com.xdd.pantry.domain.receipts.DraftStatus
import com.xdd.pantry.domain.receipts.ReceiptDraft
import com.xdd.pantry.domain.receipts.ReceiptDraftNotFoundException
import com.xdd.pantry.domain.receipts.ReceiptDraftNotReadyException
import com.xdd.pantry.domain.receipts.RecognizedReceipt
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class UpdateReceiptDraftUseCase(
    private val drafts: ReceiptDraftRepository,
    private val accessGuard: PantryAccessGuard,
) {
    fun updateDraft(userId: UserId, pantryId: PantryId, draftId: DraftId, edited: RecognizedReceipt): ReceiptDraft {
        accessGuard.checkAccess(pantryId, userId)
        val draft = drafts.getDraft(draftId) ?: throw ReceiptDraftNotFoundException(draftId)
        if (draft.pantryId != pantryId) throw ReceiptDraftNotFoundException(draftId)
        if (draft.status != DraftStatus.READY) throw ReceiptDraftNotReadyException(draftId, draft.status)

        val lines = edited.lines.map { line ->
            DraftLine(
                id = DraftLineId(UUID.randomUUID()),
                rawText = line.rawText,
                action = line.action,
                productId = line.productId,
                proposedName = line.proposedName,
                proposedBrand = line.proposedBrand,
                quantity = line.quantity,
                confidence = line.confidence,
                expiresAt = line.expiresAt,
            )
        }
        return drafts.replaceLines(draftId, lines)
    }
}
