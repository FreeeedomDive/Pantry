package com.xdd.pantry.bootstrap.telegram

import com.xdd.pantry.domain.receipts.DraftId
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.generics.TelegramClient

const val CONFIRM_PREFIX = "confirm:"

@Component
class TelegramSender(private val telegramClient: TelegramClient) {

    fun send(chatId: Long, text: String) {
        telegramClient.execute(SendMessage.builder().chatId(chatId).text(text).build())
    }

    fun sendWithConfirm(chatId: Long, text: String, draftId: DraftId) {
        val button = InlineKeyboardButton.builder()
            .text("Подтвердить ✓")
            .callbackData("$CONFIRM_PREFIX${draftId.value}")
            .build()
        val markup = InlineKeyboardMarkup.builder().keyboardRow(InlineKeyboardRow(button)).build()
        telegramClient.execute(SendMessage.builder().chatId(chatId).text(text).replyMarkup(markup).build())
    }

    fun answerCallback(callbackQueryId: String, text: String) {
        telegramClient.execute(AnswerCallbackQuery.builder().callbackQueryId(callbackQueryId).text(text).build())
    }
}
