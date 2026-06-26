package com.xdd.pantry.application.receipts

import com.xdd.pantry.domain.receipts.DraftId
import com.xdd.pantry.domain.receipts.DraftStatus
import com.xdd.pantry.domain.receipts.ReceiptDraft

interface ReceiptDraftRepository {
    fun save(draft: ReceiptDraft): ReceiptDraft
    fun getDraft(draftId: DraftId): ReceiptDraft?
    fun updateStatus(draftId: DraftId, status: DraftStatus)
}
