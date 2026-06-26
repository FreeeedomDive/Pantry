package com.xdd.pantry.infrastructure.products

import com.xdd.pantry.application.products.ProductRepository
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.infrastructure.IntegrationTestsBase
import com.xdd.pantry.infrastructure.pantries.PantryEntity
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import java.time.Instant
import java.util.UUID

@DataJpaTest
@Import(ProductRepositoryAdapter::class)
class ProductRepositoryAdapterTest : IntegrationTestsBase() {

    @Autowired
    private lateinit var products: ProductRepository

    @Autowired
    private lateinit var em: TestEntityManager

    @Test
    fun `saves product and finds it by id`() {
        val pantryId = newPantry()
        val product = Product(ProductId(UUID.randomUUID()), pantryId, "Молоко", "Простоквашино")

        products.save(product)
        em.flush()
        em.clear()

        products.getProduct(product.id) shouldBe product
    }

    @Test
    fun `returns only products of the requested pantry`() {
        val mine = newPantry()
        val other = newPantry()
        products.save(Product(ProductId(UUID.randomUUID()), mine, "Молоко", null))
        products.save(Product(ProductId(UUID.randomUUID()), mine, "Яйца", null))
        products.save(Product(ProductId(UUID.randomUUID()), other, "Чужой товар", null))
        em.flush()
        em.clear()

        products.getPantryProducts(mine).map { it.name } shouldContainExactlyInAnyOrder listOf("Молоко", "Яйца")
    }

    @Test
    fun `getProduct returns null for unknown id`() {
        products.getProduct(ProductId(UUID.randomUUID())).shouldBeNull()
    }

    @Test
    fun `deletes product`() {
        val pantryId = newPantry()
        val product = Product(ProductId(UUID.randomUUID()), pantryId, "Молоко", null)
        products.save(product)
        em.flush()
        em.clear()

        products.delete(product.id)
        em.flush()
        em.clear()

        products.getProduct(product.id).shouldBeNull()
    }

    @Test
    fun `save updates an existing product instead of inserting a duplicate`() {
        val pantryId = newPantry()
        val product = Product(ProductId(UUID.randomUUID()), pantryId, "Молоко", null)
        products.save(product)
        em.flush()
        em.clear()

        products.save(product.copy(name = "Кефир", brand = "Био"))
        em.flush()
        em.clear()

        val updated = products.getProduct(product.id)!!
        updated.name shouldBe "Кефир"
        updated.brand shouldBe "Био"
    }

    private fun newPantry(): PantryId {
        val id = UUID.randomUUID()
        em.persist(PantryEntity(id, "Дом", Instant.now()))
        return PantryId(id)
    }
}
