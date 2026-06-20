package com.xdd.pantry.application.products

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.products.ProductNotFoundException

internal fun ProductRepository.findInPantry(productId: ProductId, pantryId: PantryId): Product? =
    getProduct(productId)?.takeIf { it.pantryId == pantryId }

internal fun ProductRepository.requireInPantry(productId: ProductId, pantryId: PantryId): Product =
    findInPantry(productId, pantryId) ?: throw ProductNotFoundException(productId)
