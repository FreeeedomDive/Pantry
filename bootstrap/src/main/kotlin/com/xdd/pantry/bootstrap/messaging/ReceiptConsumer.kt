package com.xdd.pantry.bootstrap.messaging

import com.xdd.pantry.application.pantries.GetUserPantriesUseCase
import com.xdd.pantry.application.receipts.CreateReceiptDraftUseCase
import com.xdd.pantry.application.receipts.ReceiptExtractor
import com.xdd.pantry.application.receipts.RecognizeReceiptUseCase
import com.xdd.pantry.application.users.RegisterUserUseCase
import com.xdd.pantry.bootstrap.telegram.TelegramFiles
import com.xdd.pantry.bootstrap.telegram.TelegramSender
import com.xdd.pantry.domain.receipts.ExtractedReceipt
import com.xdd.pantry.domain.receipts.ReceiptImage
import com.xdd.pantry.domain.receipts.RecognizedAction
import com.xdd.pantry.domain.receipts.RecognizedReceipt
import com.xdd.pantry.domain.users.TelegramUserId
import kotlinx.coroutines.runBlocking
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class ReceiptConsumer(
    private val telegramFiles: TelegramFiles,
    private val sender: TelegramSender,
    private val registerUser: RegisterUserUseCase,
    private val getUserPantries: GetUserPantriesUseCase,
    private val extractor: ReceiptExtractor,
    private val recognizeReceipt: RecognizeReceiptUseCase,
    private val createReceiptDraft: CreateReceiptDraftUseCase,
) {
    @RabbitListener(queues = [RabbitConfig.QUEUE])
    fun onReceipt(receipt: ReceiptSubmitted) = runBlocking {
        val user = registerUser.findOrRegister(TelegramUserId(receipt.telegramUserId))
        val pantry = getUserPantries.getUserPantries(user.id).first().pantry
        val images = receipt.fileIds.map { ReceiptImage(JPEG_MIME_TYPE, telegramFiles.download(it)) }

        val extracted = extractor.extract(images)
        sender.send(receipt.chatId, formatExtracted(extracted))

        sender.send(receipt.chatId, "Сопоставляю с каталогом…")
        val recognized = recognizeReceipt.recognize(user.id, pantry.id, extracted)
        sender.send(receipt.chatId, formatRecognized(recognized))

        val draft = createReceiptDraft.createDraft(pantry.id, recognized)
        sender.sendDraftLink(receipt.chatId, "Черновик готов: ${draft.lines.size} позиц.", pantry.id, draft.id)
    }

    private fun formatExtracted(receipt: ExtractedReceipt): String =
        if (receipt.lines.isEmpty()) "Не нашёл ни одной товарной позиции"
        else "Распознал:\n" + receipt.lines.joinToString("\n") { "• ${it.rawText} — ${it.quantity} шт" }

    private fun formatRecognized(receipt: RecognizedReceipt): String =
        receipt.lines.joinToString("\n") { line ->
            when (line.action) {
                RecognizedAction.MATCH -> "✓ ${line.rawText} — ${line.quantity} шт"
                RecognizedAction.CREATE -> {
                    val brand = line.proposedBrand?.let { " · $it" } ?: ""
                    "＋ ${line.proposedName ?: line.rawText}$brand (новый) — ${line.quantity} шт"
                }
                RecognizedAction.UNSURE -> "? ${line.rawText} — ${line.quantity} шт"
            }
        }

    companion object {
        private const val JPEG_MIME_TYPE = "image/jpeg"
    }
}
