package com.xdd.pantry.domain.products

data class ProductBalance(val product: Product, val total: Int) {
    companion object {
        fun calculate(products: List<Product>, stock: List<StockItem>): List<ProductBalance> {
            val totalsByProduct = stock
                .groupBy { it.productId }
                .mapValues { (_, batches) -> batches.sumOf { it.quantity.value } }
            return products.map { product ->
                ProductBalance(product, totalsByProduct[product.id] ?: 0)
            }
        }
    }
}
