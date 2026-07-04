package com.xdd.pantry.application.receipts

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.receipts.DraftId
import com.xdd.pantry.domain.receipts.ReceiptDraft
import com.xdd.pantry.domain.receipts.ReceiptDraftNotFoundException
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service

@Service
class MatchReceiptDraftUseCase(
    private val recognizeReceipt: RecognizeReceiptUseCase,
    private val drafts: ReceiptDraftRepository,
) {
    suspend fun match(userId: UserId, pantryId: PantryId, draftId: DraftId): ReceiptDraft {
        val draft = drafts.getDraft(draftId) ?: throw ReceiptDraftNotFoundException(draftId)
        val recognized = recognizeReceipt.recognize(userId, pantryId, draft.toExtractedReceipt())
        return drafts.applyMatch(draftId, pantryId, ReceiptDraft.matchedLines(recognized))
    }
}
