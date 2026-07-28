package com.xdd.pantry.bootstrap.observability

import com.xdd.pantry.bootstrap.messaging.RabbitConfig
import com.xdd.pantry.infrastructure.telemetry.PantryTelemetry
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.AmqpAdmin
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
class QueueDepthMetrics(
    private val registry: MeterRegistry,
    private val amqpAdmin: AmqpAdmin,
) {
    private val depths = QUEUES.associateWith { AtomicLong(0) }

    @EventListener(ApplicationReadyEvent::class)
    fun registerGauges() {
        depths.forEach { (queue, depth) ->
            registry.gauge(
                PantryTelemetry.QUEUE_DEPTH,
                listOf(Tag.of(PantryTelemetry.Tag.QUEUE, queue)),
                depth,
            ) { it.get().toDouble() }
        }
        refresh()
    }

    @Scheduled(fixedDelay = REFRESH_INTERVAL_MILLIS, initialDelay = REFRESH_INTERVAL_MILLIS)
    fun refresh() {
        depths.forEach { (queue, depth) ->
            runCatching { amqpAdmin.getQueueInfo(queue)?.messageCount ?: 0L }
                .onSuccess { depth.set(it) }
                .onFailure { failure -> log.warn("Failed to read the depth of queue {}", queue, failure) }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(QueueDepthMetrics::class.java)
        private const val REFRESH_INTERVAL_MILLIS = 60_000L
        private val QUEUES = listOf(
            RabbitConfig.QUEUE,
            RabbitConfig.MATCH_QUEUE,
            RabbitConfig.DEAD_LETTER_QUEUE,
        )
    }
}
