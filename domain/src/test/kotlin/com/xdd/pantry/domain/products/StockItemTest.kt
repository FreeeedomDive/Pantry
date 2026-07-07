package com.xdd.pantry.domain.products

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class StockItemTest {

    private val stockItem = StockItem(
        id = StockItemId(UUID.randomUUID()),
        productId = ProductId(UUID.randomUUID()),
        quantity = Quantity(3),
        purchasedAt = Instant.now(),
        expiresAt = null,
    )

    @Test
    fun `write-off of a part of the batch decreases the remaining quantity`() {
        stockItem.writeOff(Quantity(2)) shouldBe stockItem.copy(quantity = Quantity(1))
    }

    @Test
    fun `write-off of the whole batch depletes it`() {
        stockItem.writeOff(Quantity(3)).shouldBeNull()
    }

    @Test
    fun `write-off of more than the batch holds depletes it`() {
        stockItem.writeOff(Quantity(5)).shouldBeNull()
    }
}
