package com.xdd.pantry.bootstrap.telegram

import com.xdd.pantry.application.pantries.GetUserPantriesUseCase
import com.xdd.pantry.application.receipts.ConfirmReceiptDraftUseCase
import com.xdd.pantry.application.receipts.CreateReceiptDraftUseCase
import com.xdd.pantry.application.receipts.ReceiptExtractor
import com.xdd.pantry.application.receipts.RecognizeReceiptUseCase
import com.xdd.pantry.application.users.RegisterUserUseCase
import com.xdd.pantry.domain.receipts.DraftId
import com.xdd.pantry.domain.receipts.ExtractedReceipt
import com.xdd.pantry.domain.receipts.ReceiptImage
import com.xdd.pantry.domain.receipts.RecognizedAction
import com.xdd.pantry.domain.receipts.RecognizedReceipt
import com.xdd.pantry.domain.users.TelegramUserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.generics.TelegramClient
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

@Component
class PantryTelegramBot(
    private val telegramClient: TelegramClient,
    private val telegramFiles: TelegramFiles,
    private val registerUser: RegisterUserUseCase,
    private val getUserPantries: GetUserPantriesUseCase,
    private val extractor: ReceiptExtractor,
    private val recognizeReceipt: RecognizeReceiptUseCase,
    private val createReceiptDraft: CreateReceiptDraftUseCase,
    private val confirmReceiptDraft: ConfirmReceiptDraftUseCase,
) : LongPollingSingleThreadUpdateConsumer {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val albums = TelegramAlbumBuffer(scope, ALBUM_WINDOW) { processReceipt(it) }

    override fun consume(update: Update) {
        when {
            update.hasCallbackQuery() -> scope.launch { onCallback(update.callbackQuery) }
            else -> update.message?.let { message -> scope.launch { route(message) } }
        }
    }

    private suspend fun route(message: Message) {
        when {
            message.hasPhoto() -> onPhoto(message)
            message.text == "/start" -> reply(message.chatId, START_TEXT)
            else -> reply(message.chatId, HINT_TEXT)
        }
    }

    private suspend fun onPhoto(message: Message) {
        if (message.mediaGroupId == null) processReceipt(listOf(message))
        else albums.add(message)
    }

    private suspend fun processReceipt(photos: List<Message>) {
        val chatId = photos.first().chatId
        val telegramUserId = TelegramUserId(photos.first().from.id)
        reply(chatId, if (photos.size == 1) "Получил, распознаю…" else "Получил ${photos.size} фото, распознаю…")

        runCatching {
            val user = registerUser.findOrRegister(telegramUserId)
            val pantry = getUserPantries.getUserPantries(user.id).first()

            val extracted = extractor.extract(photos.map { toImage(it) })
            reply(chatId, formatExtracted(extracted))

            reply(chatId, "Сопоставляю с каталогом…")
            val recognized = recognizeReceipt.recognize(user.id, pantry.id, extracted)
            reply(chatId, formatRecognized(recognized))

            val draft = createReceiptDraft.createDraft(pantry.id, recognized)
            replyWithConfirm(chatId, "Черновик готов: ${draft.lines.size} позиц.", draft.id)
        }.onFailure { failure ->
            log.error("Failed to process receipt", failure)
            reply(chatId, "Не получилось обработать, попробуй ещё раз")
        }
    }

    private fun onCallback(query: CallbackQuery) {
        val data = query.data ?: return
        if (!data.startsWith(CONFIRM_PREFIX)) return
        val chatId = query.from.id
        runCatching {
            val userId = registerUser.findOrRegister(TelegramUserId(query.from.id)).id
            val draftId = DraftId(UUID.fromString(data.removePrefix(CONFIRM_PREFIX)))
            confirmReceiptDraft.confirmDraft(userId, draftId)
        }.onSuccess {
            answerCallback(query.id, "Подтверждено")
            reply(chatId, "Черновик подтверждён — позиции начислены в инвентарь ✓")
        }.onFailure { failure ->
            log.error("Failed to confirm draft", failure)
            answerCallback(query.id, "Не удалось подтвердить (возможно, уже подтверждён)")
        }
    }

    private fun toImage(message: Message): ReceiptImage {
        val largest = message.photo.maxBy { it.fileSize ?: 0 }
        return ReceiptImage(JPEG_MIME_TYPE, telegramFiles.download(largest.fileId))
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

    private fun reply(chatId: Long, text: String) {
        telegramClient.execute(SendMessage.builder().chatId(chatId).text(text).build())
    }

    private fun replyWithConfirm(chatId: Long, text: String, draftId: DraftId) {
        val button = InlineKeyboardButton.builder()
            .text("Подтвердить ✓")
            .callbackData("$CONFIRM_PREFIX${draftId.value}")
            .build()
        val markup = InlineKeyboardMarkup.builder().keyboardRow(InlineKeyboardRow(button)).build()
        telegramClient.execute(SendMessage.builder().chatId(chatId).text(text).replyMarkup(markup).build())
    }

    private fun answerCallback(callbackQueryId: String, text: String) {
        telegramClient.execute(AnswerCallbackQuery.builder().callbackQueryId(callbackQueryId).text(text).build())
    }

    companion object {
        private val log = LoggerFactory.getLogger(PantryTelegramBot::class.java)
        private const val JPEG_MIME_TYPE = "image/jpeg"
        private const val CONFIRM_PREFIX = "confirm:"
        private val ALBUM_WINDOW = 1500.milliseconds
        private const val HINT_TEXT = "Пришли фото чека или скриншоты заказа — покажу, какие позиции я в них вижу"
        private val START_TEXT = """
            Привет! Я учётчик домашних запасов.
            Пришли фото чека или скриншоты заказа из доставки (можно несколько) — распознаю и соберу черновик поступления.
        """.trimIndent()
    }
}
