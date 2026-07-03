package com.xdd.pantry.bootstrap.telegram

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.receipts.DraftId
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo
import org.telegram.telegrambots.meta.generics.TelegramClient

@Component
class TelegramSender(
    private val telegramClient: TelegramClient,
    private val properties: TelegramProperties,
) {

    fun send(chatId: Long, text: String) {
        telegramClient.execute(SendMessage.builder().chatId(chatId).text(text).build())
    }

    fun sendDraftLink(chatId: Long, text: String, pantryId: PantryId, draftId: DraftId) {
        val url = "${properties.webAppUrl.trimEnd('/')}/pantries/${pantryId.value}/drafts/${draftId.value}"
        val button = InlineKeyboardButton.builder()
            .text("Открыть черновик")
            .webApp(WebAppInfo(url))
            .build()
        val markup = InlineKeyboardMarkup.builder().keyboardRow(InlineKeyboardRow(button)).build()
        telegramClient.execute(SendMessage.builder().chatId(chatId).text(text).replyMarkup(markup).build())
    }
}
