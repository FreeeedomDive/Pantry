package com.xdd.pantry.application.receipts

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.receipts.ReceiptDraft
import com.xdd.pantry.domain.receipts.RecognizedReceipt
import org.springframework.stereotype.Service

@Service
class CreateReceiptDraftUseCase(
    private val drafts: ReceiptDraftRepository,
) {
    fun createDraft(pantryId: PantryId, recognized: RecognizedReceipt): ReceiptDraft =
        drafts.save(ReceiptDraft.ready(pantryId, recognized))
}
