package com.xdd.pantry.application.products

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.products.StockItem
import com.xdd.pantry.domain.products.StockItemId

interface StockRepository {
    fun save(stockItem: StockItem): StockItem
    fun getStockItem(stockItemId: StockItemId): StockItem?
    fun getProductStock(productId: ProductId): List<StockItem>
    fun getPantryStock(pantryId: PantryId): List<StockItem>
    fun delete(stockItemId: StockItemId)
}
