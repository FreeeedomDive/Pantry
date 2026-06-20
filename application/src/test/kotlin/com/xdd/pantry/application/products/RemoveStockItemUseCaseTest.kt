package com.xdd.pantry.application.products

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.domain.pantries.PantryActionDeniedException
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.products.Quantity
import com.xdd.pantry.domain.products.StockItem
import com.xdd.pantry.domain.products.StockItemId
import com.xdd.pantry.domain.users.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class RemoveStockItemUseCaseTest {

    private val products = mockk<ProductRepository>()
    private val stock = mockk<StockRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val useCase = RemoveStockItemUseCase(products, stock, guard)

    private val pantryId = PantryId(UUID.randomUUID())
    private val productId = ProductId(UUID.randomUUID())
    private val stockItemId = StockItemId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())
    private val stockItem = StockItem(stockItemId, productId, Quantity(2), Instant.now(), null)

    @Test
    fun `deletes a stock item whose product is in the pantry`() {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        every { stock.getStockItem(stockItemId) } returns stockItem
        every { products.getProduct(productId) } returns Product(productId, pantryId, "Молоко", null)
        justRun { stock.delete(stockItemId) }

        useCase.removeStockItem(userId, pantryId, stockItemId)

        verify(exactly = 1) { stock.delete(stockItemId) }
    }

    @Test
    fun `does nothing when stock item does not exist`() {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        every { stock.getStockItem(stockItemId) } returns null

        useCase.removeStockItem(userId, pantryId, stockItemId)

        verify(exactly = 0) { stock.delete(any()) }
    }

    @Test
    fun `does nothing when the product is in another pantry`() {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        every { stock.getStockItem(stockItemId) } returns stockItem
        every { products.getProduct(productId) } returns
            Product(productId, PantryId(UUID.randomUUID()), "Молоко", null)

        useCase.removeStockItem(userId, pantryId, stockItemId)

        verify(exactly = 0) { stock.delete(any()) }
    }

    @Test
    fun `denies removal when user is not a member`() {
        every { guard.checkAccess(pantryId, userId, false) } throws
            PantryActionDeniedException(userId, pantryId)

        shouldThrow<PantryActionDeniedException> {
            useCase.removeStockItem(userId, pantryId, stockItemId)
        }

        verify(exactly = 0) { stock.getStockItem(any()) }
        verify(exactly = 0) { stock.delete(any()) }
    }

    private fun member() = PantryMember(pantryId, userId, PantryRole.MEMBER, Instant.now())
}
