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
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class GetPantryBalanceUseCaseTest {

    private val products = mockk<ProductRepository>()
    private val stock = mockk<StockRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val useCase = GetPantryBalanceUseCase(products, stock, guard)

    private val pantryId = PantryId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())

    @Test
    fun `returns balance for every product including those without stock`() {
        val milk = product("Молоко")
        val eggs = product("Яйца")
        every { guard.checkAccess(pantryId, userId, false) } returns
            PantryMember(pantryId, userId, PantryRole.MEMBER, Instant.now())
        every { products.getPantryProducts(pantryId) } returns listOf(milk, eggs)
        every { stock.getPantryStock(pantryId) } returns listOf(batch(milk.id, 2))

        val balances = useCase.getPantryBalance(userId, pantryId)

        balances.first { it.product == milk }.total shouldBe 2
        balances.first { it.product == eggs }.total shouldBe 0
    }

    @Test
    fun `denies access for non-member and does not query data`() {
        every { guard.checkAccess(pantryId, userId, false) } throws
            PantryActionDeniedException(userId, pantryId)

        shouldThrow<PantryActionDeniedException> { useCase.getPantryBalance(userId, pantryId) }

        verify(exactly = 0) { products.getPantryProducts(any()) }
        verify(exactly = 0) { stock.getPantryStock(any()) }
    }

    private fun product(name: String) =
        Product(ProductId(UUID.randomUUID()), pantryId, name, null)

    private fun batch(productId: ProductId, amount: Int) = StockItem(
        StockItemId(UUID.randomUUID()), productId, Quantity(amount), Instant.now(), null,
    )
}
