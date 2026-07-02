package com.xdd.pantry.application.receipts

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.receipts.DraftId
import com.xdd.pantry.domain.receipts.ReceiptDraft
import com.xdd.pantry.domain.receipts.ReceiptDraftNotFoundException
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetReceiptDraftUseCase(
    private val drafts: ReceiptDraftRepository,
    private val accessGuard: PantryAccessGuard,
) {
    fun getDraft(userId: UserId, pantryId: PantryId, draftId: DraftId): ReceiptDraft {
        accessGuard.checkAccess(pantryId, userId)
        val draft = drafts.getDraft(draftId) ?: throw ReceiptDraftNotFoundException(draftId)
        if (draft.pantryId != pantryId) throw ReceiptDraftNotFoundException(draftId)
        return draft
    }
}
