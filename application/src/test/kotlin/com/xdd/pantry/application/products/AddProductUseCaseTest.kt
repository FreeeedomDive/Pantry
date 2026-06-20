package com.xdd.pantry.application.products

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.domain.pantries.PantryActionDeniedException
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.users.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class AddProductUseCaseTest {

    private val products = mockk<ProductRepository>()
    private val guard = mockk<PantryAccessGuard>()
    private val useCase = AddProductUseCase(products, guard)

    private val pantryId = PantryId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())

    @Test
    fun `adds product to pantry the user belongs to`() {
        every { guard.checkAccess(pantryId, userId, false) } returns
            PantryMember(pantryId, userId, PantryRole.MEMBER, Instant.now())
        every { products.save(any()) } answers { firstArg() }

        val product = useCase.addProduct(userId, pantryId, "Молоко", "Простоквашино")

        product.name shouldBe "Молоко"
        product.pantryId shouldBe pantryId
        verify(exactly = 1) {
            products.save(match<Product> { it.pantryId == pantryId && it.name == "Молоко" })
        }
    }

    @Test
    fun `denies adding product when user is not a member`() {
        every { guard.checkAccess(pantryId, userId, false) } throws
            PantryActionDeniedException(userId, pantryId)

        shouldThrow<PantryActionDeniedException> {
            useCase.addProduct(userId, pantryId, "Молоко", null)
        }

        verify(exactly = 0) { products.save(any()) }
    }
}
