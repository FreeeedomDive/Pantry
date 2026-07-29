package com.xdd.pantry.bootstrap.observability

import com.xdd.pantry.infrastructure.telemetry.PantryTelemetry
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class TelegramMetrics(private val registry: MeterRegistry) {

    fun recordUpdate(type: String, outcome: String) {
        registry.counter(
            PantryTelemetry.TELEGRAM_UPDATES,
            PantryTelemetry.Tag.TYPE, type,
            PantryTelemetry.Tag.OUTCOME, outcome,
        ).increment()
    }
}
