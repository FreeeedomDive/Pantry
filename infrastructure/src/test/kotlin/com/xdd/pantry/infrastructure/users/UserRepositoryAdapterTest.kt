package com.xdd.pantry.infrastructure.users

import com.xdd.pantry.application.users.UserRepository
import com.xdd.pantry.domain.users.TelegramUserId
import com.xdd.pantry.domain.users.User
import com.xdd.pantry.domain.users.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import jakarta.persistence.PersistenceException
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
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserRepositoryAdapter::class)
@Testcontainers
class UserRepositoryAdapterTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:17-alpine")
    }

    @Autowired
    private lateinit var users: UserRepository

    @Autowired
    private lateinit var em: TestEntityManager

    @Test
    fun `saves user and finds it by telegram id`() {
        val user = newUser(telegramUserId = 100L)

        users.save(user)
        em.flush()
        em.clear()

        users.findByTelegramUserId(TelegramUserId(100L)) shouldBe user
    }

    @Test
    fun `returns null for unknown telegram id`() {
        users.findByTelegramUserId(TelegramUserId(999L)) shouldBe null
    }

    @Test
    fun `rejects second user with the same telegram id`() {
        users.save(newUser(telegramUserId = 7L))
        em.flush()
        em.clear()

        users.save(newUser(telegramUserId = 7L))

        shouldThrow<PersistenceException> { em.flush() }
    }

    private fun newUser(telegramUserId: Long) = User(
        id = UserId(UUID.randomUUID()),
        telegramUserId = TelegramUserId(telegramUserId),
        // Postgres хранит время с точностью до микросекунд, Instant.now() может
        // дать наносекунды — без усечения сравнение после round-trip ложно падает.
        createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
    )
}
