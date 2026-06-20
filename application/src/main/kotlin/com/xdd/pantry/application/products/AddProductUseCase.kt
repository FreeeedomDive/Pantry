package com.xdd.pantry.application.products

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class AddProductUseCase(
    private val products: ProductRepository,
    private val accessGuard: PantryAccessGuard,
) {
    fun addProduct(userId: UserId, pantryId: PantryId, name: String, brand: String?): Product {
        accessGuard.checkAccess(pantryId, userId)
        return products.save(Product(ProductId(UUID.randomUUID()), pantryId, name, brand))
    }
}
