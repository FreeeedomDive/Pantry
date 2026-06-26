package com.xdd.pantry.application.products

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.ProductAlias

interface ProductAliasRepository {
    fun getPantryAliases(pantryId: PantryId): List<ProductAlias>
    fun save(alias: ProductAlias)
}
