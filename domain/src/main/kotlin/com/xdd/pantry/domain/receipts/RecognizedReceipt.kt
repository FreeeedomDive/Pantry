package com.xdd.pantry.domain.receipts

import com.xdd.pantry.domain.products.ProductId

enum class RecognizedAction { MATCH, CREATE, UNSURE }

enum class Confidence { HIGH, LOW }

data class RecognizedLine(
    val rawText: String,
    val action: RecognizedAction,
    val productId: ProductId?,
    val proposedName: String?,
    val proposedBrand: String?,
    val quantity: Int,
    val confidence: Confidence,
)

data class RecognizedReceipt(val lines: List<RecognizedLine>)
