package com.xdd.pantry.bootstrap.observability

import com.xdd.pantry.infrastructure.telemetry.PantryTelemetry
import com.xdd.pantry.infrastructure.telemetry.histogramTimer
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@Component
class ReceiptPipelineMetrics(private val registry: MeterRegistry) {

    private val lastSuccessEpochSeconds = AtomicLong(System.currentTimeMillis() / MILLIS_PER_SECOND)
    private val lastPipelineMillis = AtomicLong(0)
    private val lastPositions = AtomicLong(0)
    private val lastStageMillis = STAGES.associateWith { AtomicLong(0) }

    init {
        Gauge.builder(PantryTelemetry.RECEIPT_LAST_SUCCESS_TIMESTAMP) { lastSuccessEpochSeconds.get().toDouble() }
            .baseUnit(SECONDS)
            .register(registry)

        Gauge.builder(PantryTelemetry.RECEIPT_LAST_PIPELINE_DURATION) { lastPipelineMillis.get() / MILLIS_PER_SECOND.toDouble() }
            .baseUnit(SECONDS)
            .register(registry)

        Gauge.builder(PantryTelemetry.RECEIPT_LAST_POSITIONS) { lastPositions.get().toDouble() }
            .register(registry)

        lastStageMillis.forEach { (stage, millis) ->
            Gauge.builder(PantryTelemetry.RECEIPT_LAST_STAGE_DURATION) { millis.get() / MILLIS_PER_SECOND.toDouble() }
                .tag(PantryTelemetry.Tag.STAGE, stage)
                .baseUnit(SECONDS)
                .register(registry)
        }
    }

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

        lastStageMillis[stage]?.set(TimeUnit.NANOSECONDS.toMillis(nanos))
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
        if (outcome == PantryTelemetry.Outcome.OK) {
            lastSuccessEpochSeconds.set(System.currentTimeMillis() / MILLIS_PER_SECOND)
        }

        val took = elapsedSince(submittedAtEpochMillis) ?: return
        histogramTimer(
            registry,
            PantryTelemetry.RECEIPT_PIPELINE_DURATION,
            PantryTelemetry.Buckets.PIPELINE,
            PantryTelemetry.Tag.OUTCOME, outcome,
        ).record(took, TimeUnit.MILLISECONDS)

        if (outcome == PantryTelemetry.Outcome.OK) {
            lastPipelineMillis.set(took)
        }
    }

    fun recordPositions(count: Int) {
        DistributionSummary.builder(PantryTelemetry.RECEIPT_POSITIONS)
            .serviceLevelObjectives(*POSITION_BUCKETS)
            .register(registry)
            .record(count.toDouble())

        lastPositions.set(count.toLong())
    }

    private fun elapsedSince(epochMillis: Long?): Long? {
        val since = epochMillis ?: return null
        return (System.currentTimeMillis() - since).takeIf { it >= 0 }
    }

    companion object {
        private const val SECONDS = "seconds"
        private const val MILLIS_PER_SECOND = 1000L
        private val POSITION_BUCKETS = doubleArrayOf(1.0, 2.0, 5.0, 10.0, 20.0, 50.0, 100.0)
        private val STAGES = listOf(PantryTelemetry.Stage.EXTRACT, PantryTelemetry.Stage.MATCH)
    }
}
