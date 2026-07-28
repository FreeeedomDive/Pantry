package com.xdd.pantry.infrastructure.receipts

import com.xdd.pantry.infrastructure.telemetry.PantryTelemetry
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
    private val metrics: GeminiMetrics,
) {
    suspend fun generateStructured(
        parts: List<Map<String, Any>>,
        responseSchema: Map<String, Any>,
        apiKey: String,
        operation: String,
    ): String {
        var attempt = 0
        val startedAt = System.nanoTime()
        while (true) {
            try {
                val response = requestGemini(parts, responseSchema, apiKey)
                metrics.recordCall(operation, properties.model, PantryTelemetry.Outcome.OK, elapsed(startedAt))
                recordTokens(operation, response)
                return response.text()
            } catch (failure: WebClientResponseException) {
                attempt++
                val retryable = failure.statusCode.is5xxServerError ||
                    failure.statusCode == HttpStatus.TOO_MANY_REQUESTS
                log.warn("Gemini responded {} on attempt {}: {}", failure.statusCode, attempt, failure.responseBodyAsString)
                if (!retryable || attempt >= MAX_ATTEMPTS) {
                    metrics.recordCall(operation, properties.model, metrics.outcomeOf(failure), elapsed(startedAt))
                    throw failure
                }
                metrics.recordRetry(operation, retryReasonFor(failure))
                delay(backoffFor(failure, attempt))
            } catch (failure: Throwable) {
                metrics.recordCall(operation, properties.model, metrics.outcomeOf(failure), elapsed(startedAt))
                throw failure
            }
        }
    }

    private suspend fun requestGemini(
        parts: List<Map<String, Any>>,
        responseSchema: Map<String, Any>,
        apiKey: String,
    ): GeminiResponse =
        geminiWebClient.post()
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

    private fun recordTokens(operation: String, response: GeminiResponse) {
        val usage = response.usageMetadata ?: return
        metrics.recordTokens(operation, properties.model, PantryTelemetry.TokenKind.INPUT, usage.promptTokenCount)
        metrics.recordTokens(operation, properties.model, PantryTelemetry.TokenKind.OUTPUT, usage.candidatesTokenCount)
    }

    private fun retryReasonFor(failure: WebClientResponseException): String =
        if (failure.statusCode == HttpStatus.TOO_MANY_REQUESTS) {
            PantryTelemetry.RetryReason.RATE_LIMITED
        } else {
            PantryTelemetry.RetryReason.TRANSIENT
        }

    private fun backoffFor(failure: WebClientResponseException, attempt: Int): Duration {
        val retryAfter = failure.headers.getFirst("Retry-After")?.toLongOrNull()?.seconds
        val jitter = Random.nextInt(250, 750).milliseconds
        return (retryAfter ?: (RETRY_BACKOFF * attempt)) + jitter
    }

    private fun elapsed(startedAt: Long): Long = System.nanoTime() - startedAt

    companion object {
        private val log = LoggerFactory.getLogger(GeminiClient::class.java)
        private const val MAX_ATTEMPTS = 4
        private val RETRY_BACKOFF = 2.seconds
    }
}

private data class GeminiResponse(
    val candidates: List<Candidate> = emptyList(),
    val usageMetadata: UsageMetadata? = null,
) {
    data class Candidate(val content: Content? = null)
    data class Content(val parts: List<Part> = emptyList())
    data class Part(val text: String? = null)
    data class UsageMetadata(
        val promptTokenCount: Int = 0,
        val candidatesTokenCount: Int = 0,
    )

    fun text(): String = candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "[]"
}
