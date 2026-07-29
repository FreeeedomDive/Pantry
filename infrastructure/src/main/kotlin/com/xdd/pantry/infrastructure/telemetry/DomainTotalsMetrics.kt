package com.xdd.pantry.infrastructure.telemetry

import com.xdd.pantry.infrastructure.pantries.PantryJpaRepository
import com.xdd.pantry.infrastructure.products.StockItemJpaRepository
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
class DomainTotalsMetrics(
    private val registry: MeterRegistry,
    private val pantries: PantryJpaRepository,
    private val stockItems: StockItemJpaRepository,
) {
    private val pantryCount = AtomicLong(0)
    private val stockItemCount = AtomicLong(0)

    @EventListener(ApplicationReadyEvent::class)
    fun registerGauges() {
        registry.gauge(PantryTelemetry.PANTRIES_TOTAL, pantryCount) { it.get().toDouble() }
        registry.gauge(PantryTelemetry.STOCK_ITEMS_TOTAL, stockItemCount) { it.get().toDouble() }
        refresh()
    }

    @Scheduled(fixedDelay = REFRESH_INTERVAL_MILLIS, initialDelay = REFRESH_INTERVAL_MILLIS)
    fun refresh() {
        runCatching {
            pantryCount.set(pantries.count())
            stockItemCount.set(stockItems.count())
        }.onFailure { failure -> log.warn("Failed to refresh domain total metrics", failure) }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DomainTotalsMetrics::class.java)
        private const val REFRESH_INTERVAL_MILLIS = 60_000L
    }
}
