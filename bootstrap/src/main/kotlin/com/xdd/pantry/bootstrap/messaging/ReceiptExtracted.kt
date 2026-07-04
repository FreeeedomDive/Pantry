package com.xdd.pantry.bootstrap.messaging

import java.util.UUID

data class ReceiptExtracted(
    val telegramUserId: Long,
    val chatId: Long,
    val pantryId: UUID,
    val draftId: UUID,
)
