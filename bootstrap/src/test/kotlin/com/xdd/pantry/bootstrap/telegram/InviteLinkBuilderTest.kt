package com.xdd.pantry.bootstrap.telegram

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.telegram.telegrambots.meta.api.methods.GetMe
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.generics.TelegramClient
import java.util.UUID

class InviteLinkBuilderTest {

    private val telegramClient = mockk<TelegramClient>()
    private val token = UUID.fromString("c9818f2b-60f5-4cbf-b31d-84f27aa55bc7")

    @Test
    fun `uses configured bot username without calling Telegram`() {
        val builder = InviteLinkBuilder(
            telegramClient,
            TelegramProperties(botUsername = "pantry_e2e"),
        )

        builder.buildLink(token) shouldBe
            "https://t.me/pantry_e2e?start=c9818f2b-60f5-4cbf-b31d-84f27aa55bc7"
        verify(exactly = 0) { telegramClient.execute(any<GetMe>()) }
    }

    @Test
    fun `falls back to Telegram when bot username is absent`() {
        val bot = mockk<User>()
        every { bot.userName } returns "production_bot"
        every { telegramClient.execute(any<GetMe>()) } returns bot
        val builder = InviteLinkBuilder(telegramClient, TelegramProperties())

        builder.buildLink(token) shouldBe
            "https://t.me/production_bot?start=c9818f2b-60f5-4cbf-b31d-84f27aa55bc7"
        verify(exactly = 1) { telegramClient.execute(any<GetMe>()) }
    }
}
