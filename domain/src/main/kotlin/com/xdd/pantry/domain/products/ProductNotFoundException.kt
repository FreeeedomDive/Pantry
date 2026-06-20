package com.xdd.pantry.domain.products

import com.xdd.pantry.domain.exceptions.PantryDomainException

class ProductNotFoundException(val productId: ProductId) :
    PantryDomainException("Product $productId not found")
