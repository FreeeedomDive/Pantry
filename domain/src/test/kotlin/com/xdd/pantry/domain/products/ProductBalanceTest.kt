package com.xdd.pantry.domain.products

import com.xdd.pantry.domain.pantries.PantryId
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ProductBalanceTest {

    private val pantryId = PantryId(UUID.randomUUID())

    @Test
    fun `sums all batches of a product`() {
        val milk = product("Молоко")
        val stock = listOf(batch(milk.id, 2), batch(milk.id, 3))

        val balances = ProductBalance.calculate(listOf(milk), stock)

        balances shouldHaveSize 1
        balances.single().total shouldBe 5
    }

    @Test
    fun `includes products without stock as zero balance`() {
        val milk = product("Молоко")
        val eggs = product("Яйца")

        val balances = ProductBalance.calculate(listOf(milk, eggs), listOf(batch(milk.id, 2)))

        balances shouldHaveSize 2
        balances.first { it.product == eggs }.total shouldBe 0
        balances.first { it.product == milk }.total shouldBe 2
    }

    @Test
    fun `returns empty list when there are no products`() {
        ProductBalance.calculate(emptyList(), emptyList()) shouldHaveSize 0
    }

    private fun product(name: String) =
        Product(ProductId(UUID.randomUUID()), pantryId, name, null)

    private fun batch(productId: ProductId, amount: Int) = StockItem(
        StockItemId(UUID.randomUUID()), productId, Quantity(amount), Instant.now(), null,
    )
}
