package com.xdd.pantry.infrastructure.products

import com.xdd.pantry.application.products.ProductAliasRepository
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.ProductAlias
import com.xdd.pantry.domain.products.ProductId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface ProductAliasJpaRepository : JpaRepository<ProductAliasEntity, ProductAliasKey> {
    fun findByKeyPantryId(pantryId: UUID): List<ProductAliasEntity>
}

@Repository
class ProductAliasRepositoryAdapter(
    private val jpa: ProductAliasJpaRepository,
) : ProductAliasRepository {

    override fun getPantryAliases(pantryId: PantryId): List<ProductAlias> =
        jpa.findByKeyPantryId(pantryId.value).map { it.toDomain() }

    override fun save(alias: ProductAlias) {
        jpa.save(alias.toEntity())
    }
}

private fun ProductAliasEntity.toDomain() =
    ProductAlias(PantryId(key.pantryId), key.alias, ProductId(productId))

private fun ProductAlias.toEntity() =
    ProductAliasEntity(ProductAliasKey(pantryId.value, alias), productId.value)
