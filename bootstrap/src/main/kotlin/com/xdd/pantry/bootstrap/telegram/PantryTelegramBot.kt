package com.xdd.pantry.bootstrap.telegram

import com.xdd.pantry.application.receipts.ConfirmReceiptDraftUseCase
import com.xdd.pantry.application.users.RegisterUserUseCase
import com.xdd.pantry.bootstrap.messaging.ReceiptPublisher
import com.xdd.pantry.bootstrap.messaging.ReceiptSubmitted
import com.xdd.pantry.domain.receipts.DraftId
import com.xdd.pantry.domain.users.TelegramUserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

@Component
class PantryTelegramBot(
    private val sender: TelegramSender,
    private val receiptPublisher: ReceiptPublisher,
    private val registerUser: RegisterUserUseCase,
    private val confirmReceiptDraft: ConfirmReceiptDraftUseCase,
) : LongPollingSingleThreadUpdateConsumer {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val albums = TelegramAlbumBuffer(scope, ALBUM_WINDOW) { submit(it) }

    override fun consume(update: Update) {
        when {
            update.hasCallbackQuery() -> scope.launch { onCallback(update.callbackQuery) }
            else -> update.message?.let { message -> scope.launch { route(message) } }
        }
    }

    private suspend fun route(message: Message) {
        when {
            message.hasPhoto() -> onPhoto(message)
            message.text == "/start" -> sender.send(message.chatId, START_TEXT)
            else -> sender.send(message.chatId, HINT_TEXT)
        }
    }

    private suspend fun onPhoto(message: Message) {
        if (message.mediaGroupId == null) submit(listOf(message))
        else albums.add(message)
    }

    private fun submit(photos: List<Message>) {
        val first = photos.first()
        val fileIds = photos.map { message -> message.photo.maxBy { it.fileSize ?: 0 }.fileId }
        receiptPublisher.publish(ReceiptSubmitted(first.from.id, first.chatId, fileIds))
        sender.send(first.chatId, "Принял, обрабатываю…")
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
            sender.answerCallback(query.id, "Подтверждено")
            sender.send(chatId, "Черновик подтверждён — позиции начислены в инвентарь ✓")
        }.onFailure { failure ->
            log.error("Failed to confirm draft", failure)
            sender.answerCallback(query.id, "Не удалось подтвердить (возможно, уже подтверждён)")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PantryTelegramBot::class.java)
        private val ALBUM_WINDOW = 1500.milliseconds
        private const val HINT_TEXT = "Пришли фото чека или скриншоты заказа — покажу, какие позиции я в них вижу"
        private val START_TEXT = """
            Я инвентарь домашних запасов.
            Пришли фото чека или скриншоты заказа из доставки — распознаю и соберу черновик поступления.
        """.trimIndent()
    }
}
