package com.xdd.pantry.infrastructure.telemetry

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.time.Duration

fun histogramTimer(
    registry: MeterRegistry,
    name: String,
    buckets: DoubleArray,
    vararg tags: String,
): Timer =
    Timer.builder(name)
        .tags(*tags)
        .serviceLevelObjectives(*buckets.map { Duration.ofMillis((it * MILLIS_PER_SECOND).toLong()) }.toTypedArray())
        .register(registry)

private const val MILLIS_PER_SECOND = 1000
