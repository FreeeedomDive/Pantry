package com.xdd.pantry.application.products

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.domain.pantries.PantryActionDeniedException
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.users.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class RemoveProductUseCaseTest {

    private val products = mockk<ProductRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val useCase = RemoveProductUseCase(products, guard)

    private val pantryId = PantryId(UUID.randomUUID())
    private val productId = ProductId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())

    @Test
    fun `deletes a product that belongs to the pantry`() {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        every { products.getProduct(productId) } returns Product(productId, pantryId, "Молоко", null)
        justRun { products.delete(productId) }

        useCase.removeProduct(userId, pantryId, productId)

        verify(exactly = 1) { products.delete(productId) }
    }

    @Test
    fun `does nothing when product belongs to another pantry`() {
        every { guard.checkAccess(pantryId, userId, false) } returns member()
        every { products.getProduct(productId) } returns
            Product(productId, PantryId(UUID.randomUUID()), "Молоко", null)

        useCase.removeProduct(userId, pantryId, productId)

        verify(exactly = 0) { products.delete(any()) }
    }

    @Test
    fun `denies removal when user is not a member`() {
        every { guard.checkAccess(pantryId, userId, false) } throws
            PantryActionDeniedException(userId, pantryId)

        shouldThrow<PantryActionDeniedException> {
            useCase.removeProduct(userId, pantryId, productId)
        }

        verify(exactly = 0) { products.delete(any()) }
    }

    private fun member() = PantryMember(pantryId, userId, PantryRole.MEMBER, Instant.now())
}
