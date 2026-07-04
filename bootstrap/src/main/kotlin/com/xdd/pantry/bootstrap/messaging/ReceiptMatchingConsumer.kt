package com.xdd.pantry.bootstrap.messaging

import com.xdd.pantry.application.receipts.MatchReceiptDraftUseCase
import com.xdd.pantry.application.receipts.ReceiptDraftRepository
import com.xdd.pantry.application.users.RegisterUserUseCase
import com.xdd.pantry.bootstrap.telegram.TelegramSender
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.receipts.DraftId
import com.xdd.pantry.domain.receipts.DraftStatus
import com.xdd.pantry.domain.receipts.RecognizedAction
import com.xdd.pantry.domain.receipts.ReceiptDraft
import com.xdd.pantry.domain.users.TelegramUserId
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class ReceiptMatchingConsumer(
    private val sender: TelegramSender,
    private val registerUser: RegisterUserUseCase,
    private val matchReceiptDraft: MatchReceiptDraftUseCase,
    private val drafts: ReceiptDraftRepository,
) {
    @RabbitListener(queues = [RabbitConfig.MATCH_QUEUE])
    fun onExtracted(event: ReceiptExtracted) {
        runBlocking {
            sender.send(event.chatId, "Сопоставляю с каталогом…")
            val user = registerUser.findOrRegister(TelegramUserId(event.telegramUserId))
            val pantryId = PantryId(event.pantryId)
            val draftId = DraftId(event.draftId)

            runCatching {
                matchReceiptDraft.match(user.id, pantryId, draftId)
            }.onSuccess { draft ->
                sender.send(event.chatId, formatMatched(draft))
                sender.sendDraftLink(event.chatId, "Черновик готов: ${draft.lines.size} позиц.", pantryId, draft.id)
            }.onFailure { failure ->
                log.error("Failed to match draft ${event.draftId}", failure)
                drafts.updateStatus(draftId, DraftStatus.FAILED)
                sender.send(event.chatId, "Не удалось сопоставить чек. Пришлите фото ещё раз.")
            }
        }
    }

    private fun formatMatched(draft: ReceiptDraft): String =
        draft.lines.joinToString("\n") { line ->
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
        private val log = LoggerFactory.getLogger(ReceiptMatchingConsumer::class.java)
    }
}
