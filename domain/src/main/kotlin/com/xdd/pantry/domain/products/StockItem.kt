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
) {
    fun writeOff(amount: Quantity): StockItem? =
        if (amount.value >= quantity.value) null
        else copy(quantity = Quantity(quantity.value - amount.value))
}
