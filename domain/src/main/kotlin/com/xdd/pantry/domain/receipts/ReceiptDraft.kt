package com.xdd.pantry.domain.receipts

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.ProductId
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@JvmInline value class DraftId(val value: UUID)
@JvmInline value class DraftLineId(val value: UUID)

enum class DraftStatus { EXTRACTED, MATCHING, READY, CONFIRMED, FAILED }

data class DraftLine(
    val id: DraftLineId,
    val rawText: String,
    val action: RecognizedAction,
    val productId: ProductId?,
    val proposedName: String?,
    val proposedBrand: String?,
    val quantity: Int,
    val confidence: Confidence,
    val expiresAt: LocalDate? = null,
)

data class ReceiptDraft(
    val id: DraftId,
    val pantryId: PantryId,
    val status: DraftStatus,
    val createdAt: Instant,
    val lines: List<DraftLine>,
) {
    fun toExtractedReceipt(): ExtractedReceipt =
        ExtractedReceipt(lines.map { ExtractedLine(it.rawText, it.quantity) })

    companion object {
        fun extracted(pantryId: PantryId, extracted: ExtractedReceipt): ReceiptDraft =
            ReceiptDraft(
                id = DraftId(UUID.randomUUID()),
                pantryId = pantryId,
                status = DraftStatus.EXTRACTED,
                createdAt = Instant.now(),
                lines = extracted.lines.map { line ->
                    DraftLine(
                        id = DraftLineId(UUID.randomUUID()),
                        rawText = line.rawText,
                        action = RecognizedAction.UNSURE,
                        productId = null,
                        proposedName = null,
                        proposedBrand = null,
                        quantity = line.quantity,
                        confidence = Confidence.LOW,
                    )
                },
            )

        fun matchedLines(recognized: RecognizedReceipt): List<DraftLine> =
            recognized.lines.map { line ->
                DraftLine(
                    id = DraftLineId(UUID.randomUUID()),
                    rawText = line.rawText,
                    action = line.action,
                    productId = line.productId,
                    proposedName = line.proposedName,
                    proposedBrand = line.proposedBrand,
                    quantity = line.quantity,
                    confidence = line.confidence,
                    expiresAt = line.expiresAt,
                )
            }
    }
}
