package com.xdd.pantry.application.products

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.domain.pantries.PantryActionDeniedException
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.products.ProductNotFoundException
import com.xdd.pantry.domain.products.Quantity
import com.xdd.pantry.domain.products.StockItem
import com.xdd.pantry.domain.products.StockItemId
import com.xdd.pantry.domain.users.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class GetProductStockUseCaseTest {

    private val products = mockk<ProductRepository>()
    private val stock = mockk<StockRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val useCase = GetProductStockUseCase(products, stock, guard)

    private val pantryId = PantryId(UUID.randomUUID())
    private val productId = ProductId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())

    @Test
    fun `returns stock for a product in the accessible pantry`() {
        val item = StockItem(StockItemId(UUID.randomUUID()), productId, Quantity(2), Instant.now(), null)
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        every { products.getProduct(productId) } returns Product(productId, pantryId, "Молоко", null)
        every { stock.getProductStock(productId) } returns listOf(item)

        useCase.getProductStock(userId, pantryId, productId) shouldBe listOf(item)
    }

    @Test
    fun `throws when product belongs to another pantry`() {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        every { products.getProduct(productId) } returns
            Product(productId, PantryId(UUID.randomUUID()), "Молоко", null)

        shouldThrow<ProductNotFoundException> {
            useCase.getProductStock(userId, pantryId, productId)
        }

        verify(exactly = 0) { stock.getProductStock(any()) }
    }

    @Test
    fun `denies access when user is not a member`() {
        every { guard.checkAccess(pantryId, userId, false) } throws
            PantryActionDeniedException(userId, pantryId)

        shouldThrow<PantryActionDeniedException> {
            useCase.getProductStock(userId, pantryId, productId)
        }

        verify(exactly = 0) { products.getProduct(any()) }
        verify(exactly = 0) { stock.getProductStock(any()) }
    }

    private fun member() = PantryMember(pantryId, userId, PantryRole.MEMBER, Instant.now())
}
