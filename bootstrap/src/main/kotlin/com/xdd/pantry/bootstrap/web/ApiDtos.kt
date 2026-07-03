package com.xdd.pantry.bootstrap.web

import com.xdd.pantry.domain.pantries.Pantry
import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.products.ProductBalance
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.products.StockItem
import com.xdd.pantry.domain.receipts.Confidence
import com.xdd.pantry.domain.receipts.DraftLine
import com.xdd.pantry.domain.receipts.ReceiptDraft
import com.xdd.pantry.domain.receipts.RecognizedAction
import com.xdd.pantry.domain.receipts.RecognizedLine
import com.xdd.pantry.domain.receipts.RecognizedReceipt
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class PantryResponse(val id: UUID, val name: String)
data class ProductResponse(val id: UUID, val name: String, val brand: String?)
data class ProductBalanceResponse(val product: ProductResponse, val total: Int)
data class StockItemResponse(val id: UUID, val quantity: Int, val purchasedAt: Instant, val expiresAt: LocalDate?)
data class DraftLineResponse(
    val id: UUID,
    val rawText: String,
    val action: String,
    val productId: UUID?,
    val proposedName: String?,
    val proposedBrand: String?,
    val quantity: Int,
    val confidence: String,
    val expiresAt: LocalDate?,
)
data class DraftResponse(val id: UUID, val pantryId: UUID, val status: String, val lines: List<DraftLineResponse>)

data class CreatePantryRequest(val name: String)
data class AddProductRequest(val name: String, val brand: String?)
data class RenameProductRequest(val name: String, val brand: String?)
data class AddStockRequest(val quantity: Int, val expiresAt: LocalDate?)

data class DraftLineInput(
    val rawText: String,
    val action: String,
    val productId: UUID?,
    val proposedName: String?,
    val proposedBrand: String?,
    val quantity: Int,
    val expiresAt: LocalDate?,
)
data class UpdateDraftRequest(val lines: List<DraftLineInput>)

fun UpdateDraftRequest.toRecognizedReceipt() = RecognizedReceipt(
    lines.map { line ->
        RecognizedLine(
            rawText = line.rawText,
            action = RecognizedAction.valueOf(line.action),
            productId = line.productId?.let { ProductId(it) },
            proposedName = line.proposedName,
            proposedBrand = line.proposedBrand,
            quantity = line.quantity,
            confidence = Confidence.HIGH,
            expiresAt = line.expiresAt,
        )
    },
)

fun Pantry.toResponse() = PantryResponse(id.value, name)
fun Product.toResponse() = ProductResponse(id.value, name, brand)
fun ProductBalance.toResponse() = ProductBalanceResponse(product.toResponse(), total)
fun StockItem.toResponse() = StockItemResponse(id.value, quantity.value, purchasedAt, expiresAt)
fun DraftLine.toResponse() = DraftLineResponse(
    id.value, rawText, action.name, productId?.value, proposedName, proposedBrand, quantity, confidence.name, expiresAt,
)
fun ReceiptDraft.toResponse() = DraftResponse(id.value, pantryId.value, status.name, lines.map { it.toResponse() })
