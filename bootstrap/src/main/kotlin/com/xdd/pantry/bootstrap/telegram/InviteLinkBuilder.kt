package com.xdd.pantry.bootstrap.telegram

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.GetMe
import org.telegram.telegrambots.meta.generics.TelegramClient
import java.util.UUID

@Component
class InviteLinkBuilder(
    private val telegramClient: TelegramClient,
    properties: TelegramProperties,
) {

    private val botUsername: String by lazy {
        properties.botUsername?.trim()?.takeIf { it.isNotEmpty() }
            ?: telegramClient.execute(GetMe()).userName
    }

    fun buildLink(token: UUID): String = "https://t.me/$botUsername?start=$token"
}
