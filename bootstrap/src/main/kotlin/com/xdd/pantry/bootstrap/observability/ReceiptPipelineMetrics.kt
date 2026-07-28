package com.xdd.pantry.bootstrap.observability

import com.xdd.pantry.infrastructure.telemetry.PantryTelemetry
import com.xdd.pantry.infrastructure.telemetry.histogramTimer
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class ReceiptPipelineMetrics(private val registry: MeterRegistry) {

    fun recordStage(stage: String, outcome: String, nanos: Long) {
        histogramTimer(
            registry,
            PantryTelemetry.RECEIPT_STAGE_DURATION,
            PantryTelemetry.Buckets.STAGE,
            PantryTelemetry.Tag.STAGE, stage,
            PantryTelemetry.Tag.OUTCOME, outcome,
        ).record(nanos, TimeUnit.NANOSECONDS)

        registry.counter(
            PantryTelemetry.RECEIPTS,
            PantryTelemetry.Tag.STAGE, stage,
            PantryTelemetry.Tag.OUTCOME, outcome,
        ).increment()
    }

    fun recordQueueAge(stage: String, publishedAtEpochMillis: Long?) {
        val waited = elapsedSince(publishedAtEpochMillis) ?: return
        histogramTimer(
            registry,
            PantryTelemetry.RECEIPT_STAGE_QUEUE_AGE,
            PantryTelemetry.Buckets.QUEUE_AGE,
            PantryTelemetry.Tag.STAGE, stage,
        ).record(waited, TimeUnit.MILLISECONDS)
    }

    fun recordPipeline(outcome: String, submittedAtEpochMillis: Long?) {
        val took = elapsedSince(submittedAtEpochMillis) ?: return
        histogramTimer(
            registry,
            PantryTelemetry.RECEIPT_PIPELINE_DURATION,
            PantryTelemetry.Buckets.PIPELINE,
            PantryTelemetry.Tag.OUTCOME, outcome,
        ).record(took, TimeUnit.MILLISECONDS)
    }

    fun recordPositions(count: Int) {
        DistributionSummary.builder(PantryTelemetry.RECEIPT_POSITIONS)
            .serviceLevelObjectives(*POSITION_BUCKETS)
            .register(registry)
            .record(count.toDouble())
    }

    private fun elapsedSince(epochMillis: Long?): Long? {
        val since = epochMillis ?: return null
        return (System.currentTimeMillis() - since).takeIf { it >= 0 }
    }

    companion object {
        private val POSITION_BUCKETS = doubleArrayOf(1.0, 2.0, 5.0, 10.0, 20.0, 50.0, 100.0)
    }
}
