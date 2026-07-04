package com.xdd.pantry.infrastructure.users

import com.xdd.pantry.application.pantries.CreateDefaultPantryOnRegistration
import com.xdd.pantry.application.pantries.CreateNewPantryUseCase
import com.xdd.pantry.application.pantries.PantryRepository
import com.xdd.pantry.application.users.RegisterUserUseCase
import com.xdd.pantry.application.users.UserDefaultsRepository
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.TelegramUserId
import com.xdd.pantry.infrastructure.IntegrationTestsBase
import com.xdd.pantry.infrastructure.pantries.PantryRepositoryAdapter
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(
    UserRepositoryAdapter::class,
    UserDefaultsRepositoryAdapter::class,
    PantryRepositoryAdapter::class,
    RegisterUserUseCase::class,
    CreateNewPantryUseCase::class,
    CreateDefaultPantryOnRegistration::class,
)
class UserRegistrationFlowTest : IntegrationTestsBase() {
    @Autowired
    private lateinit var registerUser: RegisterUserUseCase

    @Autowired
    private lateinit var pantries: PantryRepository

    @Autowired
    private lateinit var userDefaults: UserDefaultsRepository

    @Autowired
    private lateinit var em: TestEntityManager

    @Test
    fun `new user gets a default pantry, owns it and it is marked as their default`() {
        val user = registerUser.findOrRegister(TelegramUserId(100L))
        em.flush()
        em.clear()

        val userPantries = pantries.getUserPantries(user.id)
        userPantries shouldHaveSize 1
        userPantries.single().name shouldBe CreateDefaultPantryOnRegistration.DEFAULT_PANTRY_NAME
        pantries.getPantryMember(userPantries.single().id, user.id)?.role shouldBe PantryRole.OWNER
        userDefaults.getDefaultPantryId(user.id) shouldBe userPantries.single().id
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
