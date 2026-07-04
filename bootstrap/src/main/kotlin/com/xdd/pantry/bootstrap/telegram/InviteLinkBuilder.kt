package com.xdd.pantry.bootstrap.telegram

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.GetMe
import org.telegram.telegrambots.meta.generics.TelegramClient
import java.util.UUID

@Component
class InviteLinkBuilder(private val telegramClient: TelegramClient) {

    private val botUsername: String by lazy { telegramClient.execute(GetMe()).userName }

    fun buildLink(token: UUID): String = "https://t.me/$botUsername?start=$token"
}
