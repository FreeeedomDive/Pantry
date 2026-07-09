package com.xdd.pantry.application.products

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class StapleProductUseCase(
    private val products: ProductRepository,
    private val accessGuard: PantryAccessGuard
) {
    fun stapleProduct(
        userId: UserId,
        pantryId: PantryId,
        productId: ProductId,
        isStaple: Boolean,
    ): Product {
        accessGuard.checkAccess(pantryId, userId)
        val product = products.requireInPantry(productId, pantryId)
        return products.save(product.copy(isStaple = isStaple))
    }
}