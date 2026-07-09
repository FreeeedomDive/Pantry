package com.xdd.pantry.domain.products

import com.xdd.pantry.domain.pantries.PantryId
import java.util.UUID

@JvmInline value class ProductId(val value: UUID)

@JvmInline
value class Quantity(val value: Int) {
    init { require(value > 0) { "Quantity must be positive: $value" } }
}

data class Product(
    val id: ProductId,
    val pantryId: PantryId,
    val name: String,
    val brand: String?,
    val isStaple: Boolean = false,
)
