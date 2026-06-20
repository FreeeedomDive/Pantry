package com.xdd.pantry.application.products

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetPantryProductsUseCase(
    private val products: ProductRepository,
    private val accessGuard: PantryAccessGuard,
) {
    fun getPantryProducts(userId: UserId, pantryId: PantryId): List<Product> {
        accessGuard.checkAccess(pantryId, userId)
        return products.getPantryProducts(pantryId)
    }
}
