package com.xdd.pantry.application.pantries

import com.xdd.pantry.application.users.FakeUserDefaultsRepository
import com.xdd.pantry.domain.pantries.Pantry
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.users.UserId
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class DefaultPantryReassignerTest {

    private val pantries = mockk<PantryRepository>()
    private val userDefaults = FakeUserDefaultsRepository()
    private val reassigner = DefaultPantryReassigner(pantries, userDefaults)

    private val userId = UserId(UUID.randomUUID())
    private val removedPantryId = PantryId(UUID.randomUUID())

    @Test
    fun `does nothing when the removed pantry was not the default`() {
        val actualDefault = PantryId(UUID.randomUUID())
        userDefaults.setDefaultPantryId(userId, actualDefault)

        reassigner.reassignIfNeeded(userId, removedPantryId)

        userDefaults.getDefaultPantryId(userId) shouldBe actualDefault
    }

    @Test
    fun `repoints the default to another pantry when the current default was removed`() {
        val fallback = Pantry(PantryId(UUID.randomUUID()), "Дом", Instant.now())
        userDefaults.setDefaultPantryId(userId, removedPantryId)
        every { pantries.getUserPantries(userId) } returns listOf(fallback)

        reassigner.reassignIfNeeded(userId, removedPantryId)

        userDefaults.getDefaultPantryId(userId) shouldBe fallback.id
    }

    @Test
    fun `clears the default when no other pantry remains`() {
        userDefaults.setDefaultPantryId(userId, removedPantryId)
        every { pantries.getUserPantries(userId) } returns emptyList()

        reassigner.reassignIfNeeded(userId, removedPantryId)

        userDefaults.getDefaultPantryId(userId) shouldBe null
    }
}
