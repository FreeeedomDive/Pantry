package com.xdd.pantry.domain.products

import com.xdd.pantry.domain.pantries.PantryId

data class ProductAlias(val pantryId: PantryId, val alias: String, val productId: ProductId)

fun normalizeAlias(raw: String): String =
    raw.trim().replace(Regex("\\s+"), " ").uppercase()
