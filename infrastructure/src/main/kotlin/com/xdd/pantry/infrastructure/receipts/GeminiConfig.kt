package com.xdd.pantry.infrastructure.receipts

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableConfigurationProperties(GeminiProperties::class)
class GeminiConfig {
    @Bean
    fun geminiWebClient(properties: GeminiProperties): WebClient =
        WebClient.builder().baseUrl(properties.baseUrl).build()
}
