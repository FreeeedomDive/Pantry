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
import io.mockk.Called
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class WriteOffStockUseCaseTest {

    private val products = mockk<ProductRepository>()
    private val stock = mockk<StockRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val useCase = WriteOffStockUseCase(products, stock, guard)

    private val pantryId = PantryId(UUID.randomUUID())
    private val productId = ProductId(UUID.randomUUID())
    private val stockItemId = StockItemId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())
    private val stockItem = StockItem(stockItemId, productId, Quantity(3), Instant.now(), null)

    @Test
    fun `decreases the batch quantity when part of it is written off`() {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        every { stock.getStockItem(stockItemId) } returns stockItem
        every { products.getProduct(productId) } returns Product(productId, pantryId, "Молоко", null)
        justRun { stock.updateQuantity(stockItemId, Quantity(1)) }

        useCase.writeOffStock(userId, pantryId, stockItemId, Quantity(2))

        verify(exactly = 1) { stock.updateQuantity(stockItemId, Quantity(1)) }
        verify(exactly = 0) { stock.delete(any()) }
    }

    @Test
    fun `deletes the batch when the whole remaining quantity is written off`() {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        every { stock.getStockItem(stockItemId) } returns stockItem
        every { products.getProduct(productId) } returns Product(productId, pantryId, "Молоко", null)
        justRun { stock.delete(stockItemId) }

        useCase.writeOffStock(userId, pantryId, stockItemId, Quantity(3))

        verify(exactly = 1) { stock.getStockItem(stockItemId) }
        verify(exactly = 1) { stock.delete(stockItemId) }
        confirmVerified(stock)
    }

    @Test
    fun `deletes the batch when more than the remaining quantity is written off`() {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        every { stock.getStockItem(stockItemId) } returns stockItem
        every { products.getProduct(productId) } returns Product(productId, pantryId, "Молоко", null)
        justRun { stock.delete(stockItemId) }

        useCase.writeOffStock(userId, pantryId, stockItemId, Quantity(5))

        verify(exactly = 1) { stock.getStockItem(stockItemId) }
        verify(exactly = 1) { stock.delete(stockItemId) }
        confirmVerified(stock)
    }

    @Test
    fun `does nothing when stock item does not exist`() {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        every { stock.getStockItem(stockItemId) } returns null

        useCase.writeOffStock(userId, pantryId, stockItemId, Quantity(1))

        verify(exactly = 1) { stock.getStockItem(stockItemId) }
        confirmVerified(stock)
    }

    @Test
    fun `does nothing when the product is in another pantry`() {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        every { stock.getStockItem(stockItemId) } returns stockItem
        every { products.getProduct(productId) } returns
            Product(productId, PantryId(UUID.randomUUID()), "Молоко", null)

        useCase.writeOffStock(userId, pantryId, stockItemId, Quantity(1))

        verify(exactly = 1) { stock.getStockItem(stockItemId) }
        confirmVerified(stock)
    }

    @Test
    fun `denies write-off when user is not a member`() {
        every { guard.checkAccess(pantryId, userId, false) } throws
            PantryActionDeniedException(userId, pantryId)

        shouldThrow<PantryActionDeniedException> {
            useCase.writeOffStock(userId, pantryId, stockItemId, Quantity(1))
        }

        verify { stock wasNot Called }
    }

    private fun member() = PantryMember(pantryId, userId, PantryRole.MEMBER, Instant.now())
}
