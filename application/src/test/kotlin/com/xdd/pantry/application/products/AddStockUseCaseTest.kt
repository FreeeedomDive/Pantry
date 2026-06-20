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
import com.xdd.pantry.domain.users.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class AddStockUseCaseTest {

    private val products = mockk<ProductRepository>()
    private val stock = mockk<StockRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val useCase = AddStockUseCase(products, stock, guard)

    private val pantryId = PantryId(UUID.randomUUID())
    private val productId = ProductId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())
    private val product = Product(productId, pantryId, "Молоко", null)

    @Test
    fun `adds a stock batch when the product belongs to the accessible pantry`() {
        every { guard.checkAccess(pantryId, userId, false) } returns
            PantryMember(pantryId, userId, PantryRole.MEMBER, Instant.now())
        every { products.getProduct(productId) } returns product
        every { stock.save(any()) } answers { firstArg() }

        val item = useCase.addStock(userId, pantryId, productId, Quantity(2))

        item.productId shouldBe productId
        verify(exactly = 1) { stock.save(any<StockItem>()) }
    }

    @Test
    fun `throws when product does not exist`() {
        every { guard.checkAccess(pantryId, userId, false) } returns
            PantryMember(pantryId, userId, PantryRole.MEMBER, Instant.now())
        every { products.getProduct(productId) } returns null

        shouldThrow<ProductNotFoundException> {
            useCase.addStock(userId, pantryId, productId, Quantity(1))
        }

        verify(exactly = 0) { stock.save(any()) }
    }

    @Test
    fun `throws when product belongs to another pantry`() {
        every { guard.checkAccess(pantryId, userId, false) } returns
            PantryMember(pantryId, userId, PantryRole.MEMBER, Instant.now())
        val foreignProduct = Product(productId, PantryId(UUID.randomUUID()), "Молоко", null)
        every { products.getProduct(productId) } returns foreignProduct

        shouldThrow<ProductNotFoundException> {
            useCase.addStock(userId, pantryId, productId, Quantity(1))
        }

        verify(exactly = 0) { stock.save(any()) }
    }

    @Test
    fun `denies adding stock when user is not a member of the pantry`() {
        every { guard.checkAccess(pantryId, userId, false) } throws
            PantryActionDeniedException(userId, pantryId)

        shouldThrow<PantryActionDeniedException> {
            useCase.addStock(userId, pantryId, productId, Quantity(1))
        }

        verify(exactly = 0) { products.getProduct(any()) }
        verify(exactly = 0) { stock.save(any()) }
    }
}
