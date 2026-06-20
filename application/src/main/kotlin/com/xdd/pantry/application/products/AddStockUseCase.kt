package com.xdd.pantry.application.products

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.products.Quantity
import com.xdd.pantry.domain.products.StockItem
import com.xdd.pantry.domain.products.StockItemId
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional
class AddStockUseCase(
    private val products: ProductRepository,
    private val stock: StockRepository,
    private val accessGuard: PantryAccessGuard,
) {
    fun addStock(
        userId: UserId,
        pantryId: PantryId,
        productId: ProductId,
        quantity: Quantity,
        expiresAt: LocalDate? = null,
    ): StockItem {
        accessGuard.checkAccess(pantryId, userId)
        products.requireInPantry(productId, pantryId)
        return stock.save(
            StockItem(StockItemId(UUID.randomUUID()), productId, quantity, Instant.now(), expiresAt)
        )
    }
}
