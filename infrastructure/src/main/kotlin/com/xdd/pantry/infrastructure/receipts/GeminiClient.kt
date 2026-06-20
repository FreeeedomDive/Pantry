package com.xdd.pantry.infrastructure.receipts

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Component
class GeminiClient(
    private val geminiWebClient: WebClient,
    private val properties: GeminiProperties,
) {
    suspend fun generateStructured(
        parts: List<Map<String, Any>>,
        responseSchema: Map<String, Any>,
    ): String {
        val response = geminiWebClient.post()
            .uri { builder ->
                builder.path("/v1beta/models/${properties.model}:generateContent")
                    .queryParam("key", properties.apiKey)
                    .build()
            }
            .bodyValue(
                mapOf(
                    "contents" to listOf(mapOf("parts" to parts)),
                    "generationConfig" to mapOf(
                        "responseMimeType" to "application/json",
                        "responseSchema" to responseSchema,
                    ),
                ),
            )
            .retrieve()
            .awaitBody<GeminiResponse>()

        return response.candidates.firstOrNull()
            ?.content?.parts?.firstOrNull()?.text
            ?: "[]"
    }
}

private data class GeminiResponse(val candidates: List<Candidate> = emptyList()) {
    data class Candidate(val content: Content? = null)
    data class Content(val parts: List<Part> = emptyList())
    data class Part(val text: String? = null)
}
