package com.xdd.pantry.infrastructure.products

import com.xdd.pantry.application.products.ProductAliasRepository
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.products.ProductAlias
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.infrastructure.IntegrationTestsBase
import com.xdd.pantry.infrastructure.pantries.PantryEntity
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import java.time.Instant
import java.util.UUID

@DataJpaTest
@Import(ProductRepositoryAdapter::class, ProductAliasRepositoryAdapter::class)
class ProductAliasRepositoryAdapterTest : IntegrationTestsBase() {

    @Autowired
    private lateinit var aliases: ProductAliasRepository

    @Autowired
    private lateinit var products: ProductRepositoryAdapter

    @Autowired
    private lateinit var em: TestEntityManager

    @Test
    fun `saves an alias and reads it back for the pantry`() {
        val pantryId = newPantry()
        val product = newProduct(pantryId)
        val alias = ProductAlias(pantryId, "МОЛОКО 3.2", product.id)

        aliases.save(alias)
        em.flush()
        em.clear()

        aliases.getPantryAliases(pantryId) shouldBe listOf(alias)
    }

    @Test
    fun `getPantryAliases is scoped to the pantry`() {
        val mine = newPantry()
        val other = newPantry()
        val mineProduct = newProduct(mine)
        val otherProduct = newProduct(other)
        aliases.save(ProductAlias(mine, "МОЛОКО", mineProduct.id))
        aliases.save(ProductAlias(other, "ЧУЖОЕ", otherProduct.id))
        em.flush()
        em.clear()

        aliases.getPantryAliases(mine).map { it.alias } shouldContainExactlyInAnyOrder listOf("МОЛОКО")
    }

    private fun newPantry(): PantryId {
        val id = UUID.randomUUID()
        em.persist(PantryEntity(id, "Дом", Instant.now()))
        return PantryId(id)
    }

    private fun newProduct(pantryId: PantryId): Product =
        products.save(Product(ProductId(UUID.randomUUID()), pantryId, "Молоко", null))
}
