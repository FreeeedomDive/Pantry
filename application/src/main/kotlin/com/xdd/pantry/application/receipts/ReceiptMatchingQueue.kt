package com.xdd.pantry.application.receipts

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.receipts.DraftId
import com.xdd.pantry.domain.users.TelegramUserId

interface ReceiptMatchingQueue {
    fun requestMatching(draftId: DraftId, pantryId: PantryId, telegramUserId: TelegramUserId, chatId: Long)
}
