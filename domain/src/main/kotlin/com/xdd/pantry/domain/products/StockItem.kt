package com.xdd.pantry.domain.products

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@JvmInline value class StockItemId(val value: UUID)

data class StockItem(
    val id: StockItemId,
    val productId: ProductId,
    val quantity: Quantity,
    val purchasedAt: Instant,
    val expiresAt: LocalDate?,
)
