package com.xdd.pantry.application.receipts

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.receipts.ReceiptDraft
import com.xdd.pantry.domain.receipts.ReceiptImage
import com.xdd.pantry.domain.users.UserId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

@Service
class CreateReceiptDraftUseCase(
    private val recognizeReceipt: RecognizeReceiptUseCase,
    private val drafts: ReceiptDraftRepository,
) {
    suspend fun createDraft(userId: UserId, pantryId: PantryId, images: List<ReceiptImage>): ReceiptDraft {
        val recognized = recognizeReceipt.recognizeReceipt(userId, pantryId, images)
        val draft = ReceiptDraft.ready(pantryId, recognized)
        return withContext(Dispatchers.IO) { drafts.save(draft) }
    }
}
