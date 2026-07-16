package com.xdd.pantry.bootstrap.telegram

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.meta.generics.TelegramClient

@Configuration
@EnableConfigurationProperties(TelegramProperties::class)
class TelegramBotConfig {

    @Bean(destroyMethod = "close")
    fun telegramBotsApplication(): TelegramBotsLongPollingApplication =
        TelegramBotsLongPollingApplication()

    @Bean
    fun telegramClient(properties: TelegramProperties): TelegramClient =
        OkHttpTelegramClient(properties.botToken)

    @Bean
    fun telegramBotStarter(
        botsApplication: TelegramBotsLongPollingApplication,
        bot: PantryTelegramBot,
        properties: TelegramProperties,
    ) = ApplicationRunner {
        if (!properties.botEnabled) {
            log.info("telegram bot polling is disabled")
        } else if (properties.botToken.isBlank()) {
            log.warn("telegram.bot-token is blank, telegram bot is disabled")
        } else {
            botsApplication.registerBot(properties.botToken, bot)
            log.info("Telegram bot started")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TelegramBotConfig::class.java)
    }
}
