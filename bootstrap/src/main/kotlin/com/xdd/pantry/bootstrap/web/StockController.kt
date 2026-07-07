package com.xdd.pantry.bootstrap.web

import com.xdd.pantry.application.products.AddStockUseCase
import com.xdd.pantry.application.products.GetProductStockUseCase
import com.xdd.pantry.application.products.RemoveStockItemUseCase
import com.xdd.pantry.application.products.WriteOffStockUseCase
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.products.Quantity
import com.xdd.pantry.domain.products.StockItemId
import com.xdd.pantry.domain.users.UserId
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/pantries/{pantryId}")
class StockController(
    private val getProductStock: GetProductStockUseCase,
    private val addStock: AddStockUseCase,
    private val removeStockItem: RemoveStockItemUseCase,
    private val writeOffStock: WriteOffStockUseCase,
) {
    @GetMapping("/products/{productId}/stock")
    fun list(
        @CurrentUser userId: UserId,
        @PathVariable pantryId: UUID,
        @PathVariable productId: UUID,
    ): List<StockItemResponse> =
        getProductStock.getProductStock(userId, PantryId(pantryId), ProductId(productId)).map { it.toResponse() }

    @PostMapping("/products/{productId}/stock")
    fun add(
        @CurrentUser userId: UserId,
        @PathVariable pantryId: UUID,
        @PathVariable productId: UUID,
        @RequestBody request: AddStockRequest,
    ): StockItemResponse =
        addStock.addStock(userId, PantryId(pantryId), ProductId(productId), Quantity(request.quantity), request.expiresAt).toResponse()

    @DeleteMapping("/stock-items/{stockItemId}")
    fun remove(@CurrentUser userId: UserId, @PathVariable pantryId: UUID, @PathVariable stockItemId: UUID) {
        removeStockItem.removeStockItem(userId, PantryId(pantryId), StockItemId(stockItemId))
    }

    @PostMapping("/stock-items/{stockItemId}/write-off")
    fun writeOff(
        @CurrentUser userId: UserId,
        @PathVariable pantryId: UUID,
        @PathVariable stockItemId: UUID,
        @RequestBody request: WriteOffStockRequest,
    ) {
        writeOffStock.writeOffStock(userId, PantryId(pantryId), StockItemId(stockItemId), Quantity(request.quantity))
    }
}
