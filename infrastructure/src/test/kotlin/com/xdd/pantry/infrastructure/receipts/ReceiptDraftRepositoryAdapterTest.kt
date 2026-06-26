package com.xdd.pantry.infrastructure.receipts

import com.xdd.pantry.application.receipts.ReceiptDraftRepository
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.receipts.Confidence
import com.xdd.pantry.domain.receipts.DraftId
import com.xdd.pantry.domain.receipts.DraftLine
import com.xdd.pantry.domain.receipts.DraftLineId
import com.xdd.pantry.domain.receipts.DraftStatus
import com.xdd.pantry.domain.receipts.ReceiptDraft
import com.xdd.pantry.domain.receipts.RecognizedAction
import com.xdd.pantry.infrastructure.IntegrationTestsBase
import com.xdd.pantry.infrastructure.pantries.PantryEntity
import com.xdd.pantry.infrastructure.products.ProductRepositoryAdapter
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@DataJpaTest
@Import(ReceiptDraftRepositoryAdapter::class, ProductRepositoryAdapter::class)
class ReceiptDraftRepositoryAdapterTest : IntegrationTestsBase() {

    @Autowired
    private lateinit var drafts: ReceiptDraftRepository

    @Autowired
    private lateinit var products: ProductRepositoryAdapter

    @Autowired
    private lateinit var em: TestEntityManager

    @Test
    fun `saves a draft with lines and reads it back`() {
        val pantryId = newPantry()
        val milk = products.save(Product(ProductId(UUID.randomUUID()), pantryId, "Молоко", null))
        val draft = ReceiptDraft(
            id = DraftId(UUID.randomUUID()),
            pantryId = pantryId,
            status = DraftStatus.READY,
            createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            lines = listOf(
                DraftLine(DraftLineId(UUID.randomUUID()), "МОЛОКО 3.2", RecognizedAction.MATCH, milk.id, null, null, 1, Confidence.HIGH),
                DraftLine(DraftLineId(UUID.randomUUID()), "ХЛЕБ", RecognizedAction.CREATE, null, "Хлеб", null, 2, Confidence.LOW),
            ),
        )

        drafts.save(draft)
        em.flush()
        em.clear()

        val loaded = drafts.getDraft(draft.id)!!
        loaded.status shouldBe DraftStatus.READY
        loaded.pantryId shouldBe pantryId
        loaded.lines shouldHaveSize 2
        val matched = loaded.lines.first { it.action == RecognizedAction.MATCH }
        matched.productId shouldBe milk.id
        val created = loaded.lines.first { it.action == RecognizedAction.CREATE }
        created.productId.shouldBeNull()
        created.proposedName shouldBe "Хлеб"
        created.quantity shouldBe 2
    }

    @Test
    fun `getDraft returns null for unknown id`() {
        drafts.getDraft(DraftId(UUID.randomUUID())).shouldBeNull()
    }

    @Test
    fun `updateStatus changes the draft status`() {
        val pantryId = newPantry()
        val draft = ReceiptDraft(
            DraftId(UUID.randomUUID()), pantryId, DraftStatus.READY,
            Instant.now().truncatedTo(ChronoUnit.MICROS), emptyList(),
        )
        drafts.save(draft)
        em.flush()
        em.clear()

        drafts.updateStatus(draft.id, DraftStatus.CONFIRMED)
        em.flush()
        em.clear()

        drafts.getDraft(draft.id)!!.status shouldBe DraftStatus.CONFIRMED
    }

    private fun newPantry(): PantryId {
        val id = UUID.randomUUID()
        em.persist(PantryEntity(id, "Дом", Instant.now()))
        return PantryId(id)
    }
}
