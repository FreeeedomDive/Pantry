package com.xdd.pantry.infrastructure.receipts

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "gemini")
data class GeminiProperties(
    val recognizerApiKey: String,
    val extractorApiKey: String,
    val model: String = "gemini-flash-latest",
    val baseUrl: String = "https://generativelanguage.googleapis.com",
)
