package com.xdd.pantry.application.products

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.products.StockItem
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetProductStockUseCase(
    private val products: ProductRepository,
    private val stock: StockRepository,
    private val accessGuard: PantryAccessGuard,
) {
    fun getProductStock(userId: UserId, pantryId: PantryId, productId: ProductId): List<StockItem> {
        accessGuard.checkAccess(pantryId, userId)
        products.requireInPantry(productId, pantryId)
        return stock.getProductStock(productId)
    }
}
