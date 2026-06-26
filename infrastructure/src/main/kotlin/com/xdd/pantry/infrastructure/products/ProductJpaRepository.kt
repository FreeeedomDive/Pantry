package com.xdd.pantry.infrastructure.products

import com.xdd.pantry.application.products.ProductRepository
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.products.ProductId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

interface ProductJpaRepository : JpaRepository<ProductEntity, UUID> {
    fun findByPantryId(pantryId: UUID): List<ProductEntity>
}

@Repository
class ProductRepositoryAdapter(
    private val jpa: ProductJpaRepository,
) : ProductRepository {

    override fun save(product: Product): Product {
        val entity = jpa.findById(product.id.value).orElseGet {
            ProductEntity(product.id.value, product.pantryId.value, product.name, product.brand, Instant.now())
        }
        entity.name = product.name
        entity.brand = product.brand
        return jpa.save(entity).toDomain()
    }

    override fun getProduct(productId: ProductId): Product? =
        jpa.findById(productId.value).map { it.toDomain() }.orElse(null)

    override fun getPantryProducts(pantryId: PantryId): List<Product> =
        jpa.findByPantryId(pantryId.value).map { it.toDomain() }

    override fun delete(productId: ProductId) = jpa.deleteById(productId.value)
}

internal fun ProductEntity.toDomain() =
    Product(ProductId(id), PantryId(pantryId), name, brand)
