package com.xdd.pantry.infrastructure.receipts

import com.xdd.pantry.infrastructure.telemetry.PantryTelemetry
import com.xdd.pantry.infrastructure.telemetry.histogramTimer
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CancellationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@Component
class GeminiMetrics(private val registry: MeterRegistry) {

    private val lastCallMillis = OPERATIONS.associateWith { AtomicLong(0) }

    init {
        lastCallMillis.forEach { (operation, millis) ->
            Gauge.builder(PantryTelemetry.GEMINI_LAST_CALL_DURATION) { millis.get() / MILLIS_PER_SECOND.toDouble() }
                .tag(PantryTelemetry.Tag.OPERATION, operation)
                .baseUnit(SECONDS)
                .register(registry)
        }
    }

    fun recordCall(operation: String, model: String, outcome: String, nanos: Long) {
        registry.counter(
            PantryTelemetry.GEMINI_CALLS,
            PantryTelemetry.Tag.OPERATION, operation,
            PantryTelemetry.Tag.MODEL, model,
            PantryTelemetry.Tag.OUTCOME, outcome,
        ).increment()

        histogramTimer(
            registry,
            PantryTelemetry.GEMINI_CALL_DURATION,
            PantryTelemetry.Buckets.STAGE,
            PantryTelemetry.Tag.OPERATION, operation,
            PantryTelemetry.Tag.MODEL, model,
            PantryTelemetry.Tag.OUTCOME, outcome,
        ).record(nanos, TimeUnit.NANOSECONDS)

        lastCallMillis[operation]?.set(TimeUnit.NANOSECONDS.toMillis(nanos))
    }

    fun recordRetry(operation: String, reason: String) {
        registry.counter(
            PantryTelemetry.GEMINI_RETRIES,
            PantryTelemetry.Tag.OPERATION, operation,
            PantryTelemetry.Tag.REASON, reason,
        ).increment()
    }

    fun recordTokens(operation: String, model: String, kind: String, count: Int) {
        if (count <= 0) return
        registry.counter(
            PantryTelemetry.GEMINI_TOKENS,
            PantryTelemetry.Tag.OPERATION, operation,
            PantryTelemetry.Tag.MODEL, model,
            PantryTelemetry.Tag.KIND, kind,
        ).increment(count.toDouble())
    }

    fun outcomeOf(failure: Throwable): String = when {
        failure is CancellationException -> PantryTelemetry.Outcome.CANCELLED
        failure is WebClientResponseException -> when (failure.statusCode) {
            HttpStatus.TOO_MANY_REQUESTS -> PantryTelemetry.Outcome.RATE_LIMITED
            HttpStatus.UNAUTHORIZED -> PantryTelemetry.Outcome.UNAUTHORIZED
            HttpStatus.FORBIDDEN -> PantryTelemetry.Outcome.FORBIDDEN
            HttpStatus.NOT_FOUND -> PantryTelemetry.Outcome.NOT_FOUND
            HttpStatus.BAD_REQUEST -> PantryTelemetry.Outcome.BAD_REQUEST
            else -> if (failure.statusCode.is5xxServerError) {
                PantryTelemetry.Outcome.UPSTREAM_ERROR
            } else {
                PantryTelemetry.Outcome.ERROR
            }
        }
        else -> PantryTelemetry.Outcome.ERROR
    }

    companion object {
        private const val SECONDS = "seconds"
        private const val MILLIS_PER_SECOND = 1000L
        private val OPERATIONS = listOf(PantryTelemetry.Operation.EXTRACT, PantryTelemetry.Operation.RECOGNIZE)
    }
}
