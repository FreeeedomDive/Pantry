package com.xdd.pantry.application.users

import com.xdd.pantry.domain.users.TelegramUserId
import com.xdd.pantry.domain.users.User
import com.xdd.pantry.domain.users.UserId
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class RegisterUserUseCaseTest {

    private val users = mockk<UserRepository>()
    private val useCase = RegisterUserUseCase(users)

    private val telegramUserId = TelegramUserId(42L)

    @Test
    fun `returns existing user without registering a new one`() {
        val existing = User(UserId(UUID.randomUUID()), telegramUserId, Instant.now())
        every { users.findByTelegramUserId(telegramUserId) } returns existing

        val result = useCase.findOrRegister(telegramUserId)

        result shouldBe existing
        verify(exactly = 0) { users.save(any()) }
    }

    @Test
    fun `registers new user when telegram id is unknown`() {
        every { users.findByTelegramUserId(telegramUserId) } returns null
        every { users.save(any()) } answers { firstArg() }

        val result = useCase.findOrRegister(telegramUserId)

        result.telegramUserId shouldBe telegramUserId
        verify(exactly = 1) { users.save(match { it.telegramUserId == telegramUserId }) }
    }
}
