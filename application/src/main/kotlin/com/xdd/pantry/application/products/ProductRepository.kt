package com.xdd.pantry.application.products

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.products.ProductId

interface ProductRepository {
    fun save(product: Product): Product
    fun getProduct(productId: ProductId): Product?
    fun getPantryProducts(pantryId: PantryId): List<Product>
    fun delete(productId: ProductId)
}
