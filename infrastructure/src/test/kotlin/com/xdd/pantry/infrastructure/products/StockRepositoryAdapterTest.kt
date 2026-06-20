package com.xdd.pantry.infrastructure.products

import com.xdd.pantry.application.products.ProductRepository
import com.xdd.pantry.application.products.StockRepository
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.products.Quantity
import com.xdd.pantry.domain.products.StockItem
import com.xdd.pantry.domain.products.StockItemId
import com.xdd.pantry.infrastructure.IntegrationTestsBase
import com.xdd.pantry.infrastructure.pantries.PantryEntity
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@DataJpaTest
@Import(ProductRepositoryAdapter::class, StockRepositoryAdapter::class)
class StockRepositoryAdapterTest : IntegrationTestsBase() {

    @Autowired
    private lateinit var products: ProductRepository

    @Autowired
    private lateinit var stock: StockRepository

    @Autowired
    private lateinit var em: TestEntityManager

    @Test
    fun `saves a batch and finds it by product`() {
        val product = newProduct(newPantry())
        val item = stockItem(product.id, 2)

        stock.save(item)
        em.flush()
        em.clear()

        stock.getProductStock(product.id) shouldBe listOf(item)
    }

    @Test
    fun `getPantryStock returns batches of all products in the pantry`() {
        val pantry = newPantry()
        val milk = newProduct(pantry, "Молоко")
        val eggs = newProduct(pantry, "Яйца")
        stock.save(stockItem(milk.id, 2))
        stock.save(stockItem(milk.id, 1))
        stock.save(stockItem(eggs.id, 10))
        em.flush()
        em.clear()

        stock.getPantryStock(pantry).map { it.productId } shouldContainExactlyInAnyOrder
            listOf(milk.id, milk.id, eggs.id)
    }

    @Test
    fun `getPantryStock is scoped to the pantry`() {
        val pantry = newPantry()
        val mine = newProduct(pantry, "Молоко")
        val theirs = newProduct(newPantry(), "Чужое молоко")
        stock.save(stockItem(mine.id, 2))
        stock.save(stockItem(theirs.id, 5))
        em.flush()
        em.clear()

        stock.getPantryStock(pantry).map { it.productId } shouldContainExactlyInAnyOrder listOf(mine.id)
    }

    @Test
    fun `deletes a batch`() {
        val product = newProduct(newPantry())
        val item = stockItem(product.id, 2)
        stock.save(item)
        em.flush()
        em.clear()

        stock.delete(item.id)
        em.flush()
        em.clear()

        stock.getStockItem(item.id).shouldBeNull()
        stock.getProductStock(product.id) shouldHaveSize 0
    }

    private fun newPantry(): PantryId {
        val id = UUID.randomUUID()
        em.persist(PantryEntity(id, "Дом", Instant.now()))
        return PantryId(id)
    }

    private fun newProduct(pantryId: PantryId, name: String = "Молоко"): Product =
        products.save(Product(ProductId(UUID.randomUUID()), pantryId, name, null))

    private fun stockItem(productId: ProductId, amount: Int) = StockItem(
        id = StockItemId(UUID.randomUUID()),
        productId = productId,
        quantity = Quantity(amount),
        // timestamptz хранит микросекунды — усекаем, иначе round-trip-сравнение ложно падает
        purchasedAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
        expiresAt = null,
    )
}
