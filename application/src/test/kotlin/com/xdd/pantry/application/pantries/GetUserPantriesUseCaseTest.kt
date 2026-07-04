package com.xdd.pantry.application.pantries

import com.xdd.pantry.application.users.FakeUserDefaultsRepository
import com.xdd.pantry.domain.pantries.Pantry
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class GetUserPantriesUseCaseTest {

    private val pantries = mockk<PantryRepository>()
    private val userDefaults = FakeUserDefaultsRepository()
    private val useCase = GetUserPantriesUseCase(pantries, userDefaults)

    private val userId = UserId(UUID.randomUUID())
    private val home = Pantry(PantryId(UUID.randomUUID()), "Дом", Instant.now())
    private val shared = Pantry(PantryId(UUID.randomUUID()), "Дача", Instant.now())

    @Test
    fun `returns pantries with the caller role and marks the default one`() {
        every { pantries.getUserMemberships(userId) } returns listOf(
            PantryMember(home.id, userId, PantryRole.OWNER, Instant.now()),
            PantryMember(shared.id, userId, PantryRole.MEMBER, Instant.now()),
        )
        every { pantries.getUserPantries(userId) } returns listOf(home, shared)
        userDefaults.setDefaultPantryId(userId, home.id)

        val result = useCase.getUserPantries(userId)

        result shouldBe listOf(
            UserPantry(home, PantryRole.OWNER, isDefault = true),
            UserPantry(shared, PantryRole.MEMBER, isDefault = false),
        )
    }

    @Test
    fun `falls back to the first pantry as default when none is explicitly set`() {
        every { pantries.getUserMemberships(userId) } returns listOf(
            PantryMember(home.id, userId, PantryRole.OWNER, Instant.now()),
            PantryMember(shared.id, userId, PantryRole.MEMBER, Instant.now()),
        )
        every { pantries.getUserPantries(userId) } returns listOf(home, shared)

        val result = useCase.getUserPantries(userId)

        result shouldBe listOf(
            UserPantry(home, PantryRole.OWNER, isDefault = true),
            UserPantry(shared, PantryRole.MEMBER, isDefault = false),
        )
    }
}
