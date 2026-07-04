package com.xdd.pantry.bootstrap.telegram

import com.xdd.pantry.application.pantries.AcceptInviteResult
import com.xdd.pantry.application.pantries.AcceptPantryInviteUseCase
import com.xdd.pantry.bootstrap.messaging.ReceiptPublisher
import com.xdd.pantry.bootstrap.messaging.ReceiptSubmitted
import com.xdd.pantry.domain.users.TelegramUserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

@Component
class PantryTelegramBot(
    private val sender: TelegramSender,
    private val receiptPublisher: ReceiptPublisher,
    private val acceptPantryInvite: AcceptPantryInviteUseCase,
) : LongPollingSingleThreadUpdateConsumer {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val albums = TelegramAlbumBuffer(scope, ALBUM_WINDOW) { submit(it) }

    override fun consume(update: Update) {
        update.message?.let { message -> scope.launch { route(message) } }
    }

    private suspend fun route(message: Message) {
        when {
            message.hasPhoto() -> onPhoto(message)
            message.text?.startsWith("/start") == true -> onStart(message)
            else -> sender.send(message.chatId, HINT_TEXT)
        }
    }

    private fun onStart(message: Message) {
        val token = message.text.removePrefix("/start").trim()
            .runCatching { UUID.fromString(this) }.getOrNull()
        if (token == null) {
            sender.send(message.chatId, START_TEXT)
            return
        }
        val text = when (val result = acceptPantryInvite.acceptInvite(TelegramUserId(message.from.id), token)) {
            is AcceptInviteResult.Joined -> "Вы присоединились к инвентарю «${result.pantry.name}» ✓"
            is AcceptInviteResult.AlreadyMember -> "Вы уже участник инвентаря «${result.pantry.name}»"
            AcceptInviteResult.InvalidInvite -> "Ссылка-приглашение недействительна или устарела"
        }
        sender.send(message.chatId, text)
    }

    private suspend fun onPhoto(message: Message) {
        if (message.mediaGroupId == null) submit(listOf(message))
        else albums.add(message)
    }

    private fun submit(photos: List<Message>) {
        val first = photos.first()
        val fileIds = photos.map { message -> message.photo.maxBy { it.fileSize ?: 0 }.fileId }
        receiptPublisher.publishSubmitted(ReceiptSubmitted(first.from.id, first.chatId, fileIds))
        sender.send(first.chatId, "Распознаю товары в чеке…")
    }

    companion object {
        private val ALBUM_WINDOW = 1500.milliseconds
        private const val HINT_TEXT = "Пришли фото чека или скриншоты заказа — покажу, какие позиции я в них вижу"
        private val START_TEXT = """
            Я инвентарь домашних запасов.
            Пришли фото чека или скриншоты заказа из доставки — распознаю и соберу черновик поступления.
        """.trimIndent()
    }
}
