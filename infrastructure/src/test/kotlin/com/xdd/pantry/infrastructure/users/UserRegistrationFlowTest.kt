package com.xdd.pantry.infrastructure.users

import com.xdd.pantry.application.pantries.CreateDefaultPantryOnRegistration
import com.xdd.pantry.application.pantries.CreateNewPantryUseCase
import com.xdd.pantry.application.pantries.PantryRepository
import com.xdd.pantry.application.users.RegisterUserUseCase
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.TelegramUserId
import com.xdd.pantry.infrastructure.pantries.PantryRepositoryAdapter
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    UserRepositoryAdapter::class,
    PantryRepositoryAdapter::class,
    RegisterUserUseCase::class,
    CreateNewPantryUseCase::class,
    CreateDefaultPantryOnRegistration::class,
)
@Testcontainers
class UserRegistrationFlowTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:17-alpine")
    }

    @Autowired
    private lateinit var registerUser: RegisterUserUseCase

    @Autowired
    private lateinit var pantries: PantryRepository

    @Autowired
    private lateinit var em: TestEntityManager

    @Test
    fun `new user gets a default pantry and owns it`() {
        val user = registerUser.findOrRegister(TelegramUserId(100L))
        em.flush()
        em.clear()

        val userPantries = pantries.getUserPantries(user.id)
        userPantries shouldHaveSize 1
        userPantries.single().name shouldBe CreateDefaultPantryOnRegistration.DEFAULT_PANTRY_NAME
        pantries.getPantryMember(userPantries.single().id, user.id)?.role shouldBe PantryRole.OWNER
    }

    @Test
    fun `repeated registration does not create a second pantry`() {
        val first = registerUser.findOrRegister(TelegramUserId(200L))
        val second = registerUser.findOrRegister(TelegramUserId(200L))
        em.flush()
        em.clear()

        second shouldBe first
        pantries.getUserPantries(first.id) shouldHaveSize 1
    }
}
