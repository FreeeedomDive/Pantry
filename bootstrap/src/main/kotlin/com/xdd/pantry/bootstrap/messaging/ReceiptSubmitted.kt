package com.xdd.pantry.bootstrap.messaging

data class ReceiptSubmitted(
    val telegramUserId: Long,
    val chatId: Long,
    val fileIds: List<String>,
)
