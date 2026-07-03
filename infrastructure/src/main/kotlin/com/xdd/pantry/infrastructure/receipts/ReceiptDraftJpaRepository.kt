package com.xdd.pantry.infrastructure.receipts

import com.xdd.pantry.application.receipts.ReceiptDraftRepository
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.receipts.Confidence
import com.xdd.pantry.domain.receipts.DraftId
import com.xdd.pantry.domain.receipts.DraftLine
import com.xdd.pantry.domain.receipts.DraftLineId
import com.xdd.pantry.domain.receipts.DraftStatus
import com.xdd.pantry.domain.receipts.ReceiptDraft
import com.xdd.pantry.domain.receipts.RecognizedAction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface ReceiptDraftJpaRepository : JpaRepository<ReceiptDraftEntity, UUID>

interface ReceiptDraftLineJpaRepository : JpaRepository<ReceiptDraftLineEntity, UUID> {
    fun findByDraftId(draftId: UUID): List<ReceiptDraftLineEntity>
    fun deleteByDraftId(draftId: UUID)
}

@Repository
class ReceiptDraftRepositoryAdapter(
    private val drafts: ReceiptDraftJpaRepository,
    private val lines: ReceiptDraftLineJpaRepository,
) : ReceiptDraftRepository {

    @Transactional
    override fun save(draft: ReceiptDraft): ReceiptDraft {
        drafts.save(draft.toEntity())
        lines.saveAll(draft.lines.map { it.toEntity(draft.id) })
        return draft
    }

    override fun getDraft(draftId: DraftId): ReceiptDraft? {
        val header = drafts.findById(draftId.value).orElse(null) ?: return null
        val draftLines = lines.findByDraftId(draftId.value).map { it.toDomain() }
        return header.toDomain(draftLines)
    }

    @Transactional
    override fun updateStatus(draftId: DraftId, status: DraftStatus) {
        val entity = drafts.findById(draftId.value).orElse(null) ?: return
        entity.status = status.name
    }

    @Transactional
    override fun replaceLines(draftId: DraftId, newLines: List<DraftLine>): ReceiptDraft {
        lines.deleteByDraftId(draftId.value)
        lines.saveAll(newLines.map { it.toEntity(draftId) })
        return getDraft(draftId) ?: error("Draft $draftId disappeared while replacing lines")
    }
}

private fun ReceiptDraftEntity.toDomain(lines: List<DraftLine>) =
    ReceiptDraft(DraftId(id), PantryId(pantryId), DraftStatus.valueOf(status), createdAt, lines)

private fun ReceiptDraft.toEntity() =
    ReceiptDraftEntity(id.value, pantryId.value, status.name, createdAt)

private fun ReceiptDraftLineEntity.toDomain() = DraftLine(
    id = DraftLineId(id),
    rawText = rawText,
    action = RecognizedAction.valueOf(action),
    productId = productId?.let { ProductId(it) },
    proposedName = proposedName,
    proposedBrand = proposedBrand,
    quantity = quantity,
    confidence = Confidence.valueOf(confidence),
    expiresAt = expiresAt,
)

private fun DraftLine.toEntity(draftId: DraftId) = ReceiptDraftLineEntity(
    id = id.value,
    draftId = draftId.value,
    rawText = rawText,
    action = action.name,
    productId = productId?.value,
    proposedName = proposedName,
    proposedBrand = proposedBrand,
    quantity = quantity,
    confidence = confidence.name,
    expiresAt = expiresAt,
)
