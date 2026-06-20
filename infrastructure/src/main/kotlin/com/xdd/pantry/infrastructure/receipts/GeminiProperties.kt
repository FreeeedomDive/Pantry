package com.xdd.pantry.infrastructure.receipts

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "gemini")
data class GeminiProperties(
    val apiKey: String,
    val model: String = "gemini-3.5-flash",
    val baseUrl: String = "https://generativelanguage.googleapis.com",
)
