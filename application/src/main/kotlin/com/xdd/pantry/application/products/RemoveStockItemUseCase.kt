package com.xdd.pantry.application.products

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.StockItemId
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RemoveStockItemUseCase(
    private val products: ProductRepository,
    private val stock: StockRepository,
    private val accessGuard: PantryAccessGuard,
) {
    fun removeStockItem(userId: UserId, pantryId: PantryId, stockItemId: StockItemId) {
        accessGuard.checkAccess(pantryId, userId)
        val stockItem = stock.getStockItem(stockItemId) ?: return
        products.findInPantry(stockItem.productId, pantryId) ?: return
        stock.delete(stockItemId)
    }
}
