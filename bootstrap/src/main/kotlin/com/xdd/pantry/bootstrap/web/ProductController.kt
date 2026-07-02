package com.xdd.pantry.bootstrap.web

import com.xdd.pantry.application.products.AddProductUseCase
import com.xdd.pantry.application.products.GetPantryBalanceUseCase
import com.xdd.pantry.application.products.GetPantryProductsUseCase
import com.xdd.pantry.application.products.RemoveProductUseCase
import com.xdd.pantry.application.products.RenameProductUseCase
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.users.UserId
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/pantries/{pantryId}")
class ProductController(
    private val getPantryProducts: GetPantryProductsUseCase,
    private val getPantryBalance: GetPantryBalanceUseCase,
    private val addProduct: AddProductUseCase,
    private val renameProduct: RenameProductUseCase,
    private val removeProduct: RemoveProductUseCase,
) {
    @GetMapping("/products")
    fun list(@CurrentUser userId: UserId, @PathVariable pantryId: UUID): List<ProductResponse> =
        getPantryProducts.getPantryProducts(userId, PantryId(pantryId)).map { it.toResponse() }

    @GetMapping("/balance")
    fun balance(@CurrentUser userId: UserId, @PathVariable pantryId: UUID): List<ProductBalanceResponse> =
        getPantryBalance.getPantryBalance(userId, PantryId(pantryId)).map { it.toResponse() }

    @PostMapping("/products")
    fun add(
        @CurrentUser userId: UserId,
        @PathVariable pantryId: UUID,
        @RequestBody request: AddProductRequest,
    ): ProductResponse =
        addProduct.addProduct(userId, PantryId(pantryId), request.name, request.brand).toResponse()

    @PatchMapping("/products/{productId}")
    fun rename(
        @CurrentUser userId: UserId,
        @PathVariable pantryId: UUID,
        @PathVariable productId: UUID,
        @RequestBody request: RenameProductRequest,
    ): ProductResponse =
        renameProduct.renameProduct(userId, PantryId(pantryId), ProductId(productId), request.name, request.brand).toResponse()

    @DeleteMapping("/products/{productId}")
    fun remove(@CurrentUser userId: UserId, @PathVariable pantryId: UUID, @PathVariable productId: UUID) {
        removeProduct.removeProduct(userId, PantryId(pantryId), ProductId(productId))
    }
}
