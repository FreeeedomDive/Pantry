package com.xdd.pantry.application.products

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.domain.pantries.PantryActionDeniedException
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.products.ProductNotFoundException
import com.xdd.pantry.domain.users.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class RenameProductUseCaseTest {

    private val products = mockk<ProductRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val useCase = RenameProductUseCase(products, guard)

    private val pantryId = PantryId(UUID.randomUUID())
    private val productId = ProductId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())
    private val product = Product(productId, pantryId, "Молоко", null)

    @Test
    fun `renames a product in the accessible pantry`() {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        every { products.getProduct(productId) } returns product
        every { products.save(any()) } answers { firstArg() }

        val result = useCase.renameProduct(userId, pantryId, productId, "Кефир", "Био")

        result.name shouldBe "Кефир"
        verify(exactly = 1) {
            products.save(match<Product> { it.name == "Кефир" && it.brand == "Био" })
        }
    }

    @Test
    fun `throws when product belongs to another pantry`() {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        every { products.getProduct(productId) } returns
            Product(productId, PantryId(UUID.randomUUID()), "Молоко", null)

        shouldThrow<ProductNotFoundException> {
            useCase.renameProduct(userId, pantryId, productId, "Кефир", null)
        }

        verify(exactly = 0) { products.save(any()) }
    }

    @Test
    fun `denies rename when user is not a member`() {
        every { guard.checkAccess(pantryId, userId, false) } throws
            PantryActionDeniedException(userId, pantryId)

        shouldThrow<PantryActionDeniedException> {
            useCase.renameProduct(userId, pantryId, productId, "Кефир", null)
        }

        verify(exactly = 0) { products.getProduct(any()) }
        verify(exactly = 0) { products.save(any()) }
    }

    private fun member() = PantryMember(pantryId, userId, PantryRole.MEMBER, Instant.now())
}
