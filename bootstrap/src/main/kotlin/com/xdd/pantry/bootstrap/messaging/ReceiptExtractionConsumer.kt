package com.xdd.pantry.bootstrap.messaging

import com.xdd.pantry.application.pantries.GetUserPantriesUseCase
import com.xdd.pantry.application.receipts.CreateReceiptDraftUseCase
import com.xdd.pantry.application.receipts.ReceiptExtractor
import com.xdd.pantry.application.users.RegisterUserUseCase
import com.xdd.pantry.application.users.UserDefaultsRepository
import com.xdd.pantry.bootstrap.telegram.TelegramFiles
import com.xdd.pantry.bootstrap.telegram.TelegramSender
import com.xdd.pantry.domain.receipts.ExtractedReceipt
import com.xdd.pantry.domain.receipts.ReceiptImage
import com.xdd.pantry.domain.users.TelegramUserId
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class ReceiptExtractionConsumer(
    private val telegramFiles: TelegramFiles,
    private val sender: TelegramSender,
    private val registerUser: RegisterUserUseCase,
    private val getUserPantries: GetUserPantriesUseCase,
    private val userDefaults: UserDefaultsRepository,
    private val extractor: ReceiptExtractor,
    private val createReceiptDraft: CreateReceiptDraftUseCase,
    private val receiptPublisher: ReceiptPublisher,
) {
    @RabbitListener(queues = [RabbitConfig.QUEUE])
    fun onReceipt(receipt: ReceiptSubmitted) {
        runBlocking {
            val user = registerUser.findOrRegister(TelegramUserId(receipt.telegramUserId))
            val pantryId = userDefaults.getDefaultPantryId(user.id)
                ?: getUserPantries.getUserPantries(user.id).first().pantry.id

            runCatching {
                val images = receipt.fileIds.map { ReceiptImage(JPEG_MIME_TYPE, telegramFiles.download(it)) }
                extractor.extract(images)
            }.onSuccess { extracted ->
                if (extracted.lines.isEmpty()) {
                    sender.send(receipt.chatId, "Не нашёл ни одной товарной позиции")
                    return@runBlocking
                }
                sender.send(receipt.chatId, formatExtracted(extracted))
                val draft = createReceiptDraft.createFromExtracted(pantryId, extracted)
                receiptPublisher.publishExtracted(
                    ReceiptExtracted(receipt.telegramUserId, receipt.chatId, pantryId.value, draft.id.value),
                )
            }.onFailure { failure ->
                log.error("Failed to extract receipt for chat ${receipt.chatId}", failure)
                sender.send(receipt.chatId, "Не удалось разобрать чек. Пришлите фото ещё раз.")
            }
        }
    }

    private fun formatExtracted(receipt: ExtractedReceipt): String =
        "Распознал:\n" + receipt.lines.joinToString("\n") { "• ${it.rawText} — ${it.quantity} шт" }

    companion object {
        private val log = LoggerFactory.getLogger(ReceiptExtractionConsumer::class.java)
        private const val JPEG_MIME_TYPE = "image/jpeg"
    }
}
