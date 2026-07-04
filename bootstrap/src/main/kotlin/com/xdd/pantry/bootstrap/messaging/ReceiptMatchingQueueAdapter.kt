package com.xdd.pantry.bootstrap.messaging

import com.xdd.pantry.application.receipts.ReceiptMatchingQueue
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.receipts.DraftId
import com.xdd.pantry.domain.users.TelegramUserId
import org.springframework.stereotype.Component

@Component
class ReceiptMatchingQueueAdapter(
    private val publisher: ReceiptPublisher,
) : ReceiptMatchingQueue {
    override fun requestMatching(draftId: DraftId, pantryId: PantryId, telegramUserId: TelegramUserId, chatId: Long) {
        publisher.publishExtracted(ReceiptExtracted(telegramUserId.value, chatId, pantryId.value, draftId.value))
    }
}
