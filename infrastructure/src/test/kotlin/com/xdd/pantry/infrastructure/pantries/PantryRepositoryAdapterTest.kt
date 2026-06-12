package com.xdd.pantry.infrastructure.pantries

import com.xdd.pantry.application.pantries.PantryRepository
import com.xdd.pantry.domain.pantries.Pantry
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId
import com.xdd.pantry.infrastructure.users.UserEntity
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
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
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PantryRepositoryAdapter::class)
@Testcontainers
class PantryRepositoryAdapterTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:17-alpine")
    }

    @Autowired
    private lateinit var pantries: PantryRepository

    @Autowired
    private lateinit var em: TestEntityManager

    @Test
    fun `saves pantry with member and finds it through membership`() {
        val owner = newUser()
        val pantry = newPantry("Наша квартира")
        pantries.save(PantryMember(pantry.id, owner, PantryRole.OWNER, now()))
        em.flush()
        em.clear()

        pantries.getUserPantries(owner) shouldBe listOf(pantry)
        pantries.getPantryMember(pantry.id, owner)?.role shouldBe PantryRole.OWNER
    }

    @Test
    fun `returns only pantries where user is a member`() {
        val alice = newUser()
        val bob = newUser()
        val shared = newPantry("Общая квартира")
        val bobsOwn = newPantry("Квартира Боба")
        pantries.save(PantryMember(shared.id, alice, PantryRole.OWNER, now()))
        pantries.save(PantryMember(shared.id, bob, PantryRole.MEMBER, now()))
        pantries.save(PantryMember(bobsOwn.id, bob, PantryRole.OWNER, now()))
        em.flush()
        em.clear()

        pantries.getUserPantries(alice).map { it.name } shouldBe listOf("Общая квартира")
        pantries.getUserPantries(bob).map { it.name } shouldContainExactlyInAnyOrder
            listOf("Общая квартира", "Квартира Боба")
    }

    @Test
    fun `returns empty list for user without memberships`() {
        val loner = newUser()

        pantries.getUserPantries(loner) shouldBe emptyList()
    }

    @Test
    fun `getPantryMembers returns every member of the pantry`() {
        val alice = newUser()
        val bob = newUser()
        val pantry = newPantry()
        pantries.save(PantryMember(pantry.id, alice, PantryRole.OWNER, now()))
        pantries.save(PantryMember(pantry.id, bob, PantryRole.MEMBER, now()))
        em.flush()
        em.clear()

        pantries.getPantryMembers(pantry.id).map { it.userId } shouldContainExactlyInAnyOrder
            listOf(alice, bob)
    }

    @Test
    fun `updates member role`() {
        val user = newUser()
        val pantry = newPantry()
        pantries.save(PantryMember(pantry.id, user, PantryRole.MEMBER, now()))
        em.flush()
        em.clear()

        pantries.updateRole(pantry.id, user, PantryRole.OWNER)
        em.flush()
        em.clear()

        pantries.getPantryMember(pantry.id, user)?.role shouldBe PantryRole.OWNER
    }

    @Test
    fun `deletes membership`() {
        val user = newUser()
        val pantry = newPantry()
        pantries.save(PantryMember(pantry.id, user, PantryRole.MEMBER, now()))
        em.flush()
        em.clear()

        pantries.deletePantryMember(pantry.id, user)
        em.flush()
        em.clear()

        pantries.getPantryMember(pantry.id, user) shouldBe null
        pantries.getUserPantries(user) shouldBe emptyList()
    }

    private fun newUser(): UserId {
        val id = UUID.randomUUID()
        em.persist(UserEntity(id, Random.nextLong(), now()))
        return UserId(id)
    }

    private fun newPantry(name: String = "Дом"): Pantry =
        pantries.save(Pantry(PantryId(UUID.randomUUID()), name, now()))

    private fun now(): Instant = Instant.now().truncatedTo(ChronoUnit.MICROS)
}
