package com.xdd.pantry.application.products

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.ProductBalance
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetPantryBalanceUseCase(
    private val products: ProductRepository,
    private val stock: StockRepository,
    private val accessGuard: PantryAccessGuard,
) {
    fun getPantryBalance(userId: UserId, pantryId: PantryId): List<ProductBalance> {
        accessGuard.checkAccess(pantryId, userId)
        return ProductBalance.calculate(
            products.getPantryProducts(pantryId),
            stock.getPantryStock(pantryId),
        )
    }
}
