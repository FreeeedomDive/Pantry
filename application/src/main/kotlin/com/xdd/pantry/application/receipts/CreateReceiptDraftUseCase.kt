package com.xdd.pantry.application.receipts

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.receipts.ExtractedReceipt
import com.xdd.pantry.domain.receipts.ReceiptDraft
import org.springframework.stereotype.Service

@Service
class CreateReceiptDraftUseCase(
    private val drafts: ReceiptDraftRepository,
) {
    fun createFromExtracted(pantryId: PantryId, extracted: ExtractedReceipt): ReceiptDraft =
        drafts.save(ReceiptDraft.extracted(pantryId, extracted))
}
