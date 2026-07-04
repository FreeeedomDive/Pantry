package com.xdd.pantry.application.pantries

import com.xdd.pantry.application.users.UserDefaultsRepository
import com.xdd.pantry.domain.pantries.Pantry
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.users.TelegramUserId
import com.xdd.pantry.domain.users.UserId
import com.xdd.pantry.domain.users.UserRegistered
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class CreateDefaultPantryOnRegistrationTest {

    private val createNewPantry = mockk<CreateNewPantryUseCase>()
    private val userDefaults = mockk<UserDefaultsRepository>()
    private val listener = CreateDefaultPantryOnRegistration(createNewPantry, userDefaults)

    @Test
    fun `creates default pantry and marks it as the user default`() {
        val userId = UserId(UUID.randomUUID())
        val pantryId = PantryId(UUID.randomUUID())
        every { createNewPantry.createNewPantry(any(), any()) } returns
            Pantry(pantryId, CreateDefaultPantryOnRegistration.DEFAULT_PANTRY_NAME, Instant.now())
        justRun { userDefaults.setDefaultPantryId(any(), any()) }

        listener.on(UserRegistered(userId, TelegramUserId(42L)))

        verify(exactly = 1) {
            createNewPantry.createNewPantry(userId, CreateDefaultPantryOnRegistration.DEFAULT_PANTRY_NAME)
        }
        verify(exactly = 1) { userDefaults.setDefaultPantryId(userId, pantryId) }
    }
}
