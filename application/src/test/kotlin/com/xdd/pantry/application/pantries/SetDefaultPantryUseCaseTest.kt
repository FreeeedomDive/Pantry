package com.xdd.pantry.application.pantries

import com.xdd.pantry.application.users.UserDefaultsRepository
import com.xdd.pantry.domain.pantries.PantryActionDeniedException
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class SetDefaultPantryUseCaseTest {

    private val guard = mockk<PantryAccessGuard>()
    private val userDefaults = mockk<UserDefaultsRepository>()
    private val useCase = SetDefaultPantryUseCase(guard, userDefaults)

    private val userId = UserId(UUID.randomUUID())
    private val pantryId = PantryId(UUID.randomUUID())

    @Test
    fun `a member can set a pantry they belong to as their default`() {
        every { guard.checkAccess(pantryId, userId) } returns
            PantryMember(pantryId, userId, PantryRole.MEMBER, Instant.now())
        justRun { userDefaults.setDefaultPantryId(userId, pantryId) }

        useCase.setDefault(userId, pantryId)

        verify(exactly = 1) { userDefaults.setDefaultPantryId(userId, pantryId) }
    }

    @Test
    fun `denies setting default for a pantry the user is not a member of`() {
        every { guard.checkAccess(pantryId, userId) } throws PantryActionDeniedException(userId, pantryId)

        shouldThrow<PantryActionDeniedException> { useCase.setDefault(userId, pantryId) }

        verify(exactly = 0) { userDefaults.setDefaultPantryId(any(), any()) }
    }
}
