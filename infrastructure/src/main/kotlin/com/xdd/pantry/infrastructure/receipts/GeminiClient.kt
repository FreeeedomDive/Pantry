package com.xdd.pantry.infrastructure.receipts

import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Component
class GeminiClient(
    private val geminiWebClient: WebClient,
    private val properties: GeminiProperties,
) {
    suspend fun generateStructured(
        parts: List<Map<String, Any>>,
        responseSchema: Map<String, Any>,
        apiKey: String,
    ): String {
        var attempt = 0
        while (true) {
            try {
                return requestGemini(parts, responseSchema, apiKey)
            } catch (failure: WebClientResponseException) {
                attempt++
                val retryable = failure.statusCode.is5xxServerError ||
                    failure.statusCode == HttpStatus.TOO_MANY_REQUESTS
                log.warn("Gemini responded {} on attempt {}: {}", failure.statusCode, attempt, failure.responseBodyAsString)
                if (!retryable || attempt >= MAX_ATTEMPTS) throw failure
                delay(backoffFor(failure, attempt))
            }
        }
    }

    private suspend fun requestGemini(
        parts: List<Map<String, Any>>,
        responseSchema: Map<String, Any>,
        apiKey: String,
    ): String {
        val response = geminiWebClient.post()
            .uri { builder ->
                builder.path("/v1beta/models/${properties.model}:generateContent")
                    .queryParam("key", apiKey)
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

    private fun backoffFor(failure: WebClientResponseException, attempt: Int): Duration {
        val retryAfter = failure.headers.getFirst("Retry-After")?.toLongOrNull()?.seconds
        val jitter = Random.nextInt(250, 750).milliseconds
        return (retryAfter ?: RETRY_BACKOFF * attempt) + jitter
    }

    companion object {
        private val log = LoggerFactory.getLogger(GeminiClient::class.java)
        private const val MAX_ATTEMPTS = 4
        private val RETRY_BACKOFF = 2.seconds
    }
}

private data class GeminiResponse(val candidates: List<Candidate> = emptyList()) {
    data class Candidate(val content: Content? = null)
    data class Content(val parts: List<Part> = emptyList())
    data class Part(val text: String? = null)
}
