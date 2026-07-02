package com.xdd.pantry.bootstrap.telegram

import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.generics.TelegramClient
import java.net.URI

@Component
class TelegramFiles(
    private val telegramClient: TelegramClient,
    private val properties: TelegramProperties,
) {
    private val rest = RestClient.builder().requestFactory(JdkClientHttpRequestFactory()).build()

    fun download(fileId: String): ByteArray {
        val file = telegramClient.execute(GetFile(fileId))
        val fileUrl = URI.create("https://api.telegram.org/file/bot${properties.botToken}/${file.filePath}")
        return rest.get().uri(fileUrl).retrieve().body<ByteArray>()
            ?: error("Telegram returned an empty file for $fileId")
    }
}
