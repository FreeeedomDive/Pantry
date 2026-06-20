package com.xdd.pantry.application.products

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RemoveProductUseCase(
    private val products: ProductRepository,
    private val accessGuard: PantryAccessGuard,
) {
    fun removeProduct(userId: UserId, pantryId: PantryId, productId: ProductId) {
        accessGuard.checkAccess(pantryId, userId)
        products.findInPantry(productId, pantryId) ?: return
        products.delete(productId)
    }
}
