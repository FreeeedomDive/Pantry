package com.xdd.pantry.infrastructure.receipts

import io.netty.resolver.DefaultAddressResolverGroup
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
@EnableConfigurationProperties(GeminiProperties::class)
class GeminiConfig {
    @Bean
    fun geminiWebClient(properties: GeminiProperties): WebClient {
        val httpClient = HttpClient.create().resolver(DefaultAddressResolverGroup.INSTANCE)
        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .baseUrl(properties.baseUrl)
            .build()
    }
}
