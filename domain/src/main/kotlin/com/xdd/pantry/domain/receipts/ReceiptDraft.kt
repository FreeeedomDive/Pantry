package com.xdd.pantry.domain.receipts

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.ProductId
import java.time.Instant
import java.util.UUID

@JvmInline value class DraftId(val value: UUID)
@JvmInline value class DraftLineId(val value: UUID)

enum class DraftStatus { PENDING, READY, CONFIRMED, FAILED }

data class DraftLine(
    val id: DraftLineId,
    val rawText: String,
    val action: RecognizedAction,
    val productId: ProductId?,
    val proposedName: String?,
    val proposedBrand: String?,
    val quantity: Int,
    val confidence: Confidence,
)

data class ReceiptDraft(
    val id: DraftId,
    val pantryId: PantryId,
    val status: DraftStatus,
    val createdAt: Instant,
    val lines: List<DraftLine>,
) {
    companion object {
        fun ready(pantryId: PantryId, recognized: RecognizedReceipt): ReceiptDraft =
            ReceiptDraft(
                id = DraftId(UUID.randomUUID()),
                pantryId = pantryId,
                status = DraftStatus.READY,
                createdAt = Instant.now(),
                lines = recognized.lines.map { line ->
                    DraftLine(
                        id = DraftLineId(UUID.randomUUID()),
                        rawText = line.rawText,
                        action = line.action,
                        productId = line.productId,
                        proposedName = line.proposedName,
                        proposedBrand = line.proposedBrand,
                        quantity = line.quantity,
                        confidence = line.confidence,
                    )
                },
            )
    }
}
