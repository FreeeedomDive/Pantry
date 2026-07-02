package com.xdd.pantry.infrastructure.receipts

import com.xdd.pantry.application.receipts.ReceiptRecognizer
import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.receipts.Confidence
import com.xdd.pantry.domain.receipts.ExtractedLine
import com.xdd.pantry.domain.receipts.RecognizedAction
import com.xdd.pantry.domain.receipts.RecognizedLine
import com.xdd.pantry.domain.receipts.RecognizedReceipt
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.UUID

@Component
class GeminiReceiptRecognizer(
    private val gemini: GeminiClient,
    private val geminiProperties: GeminiProperties,
    private val objectMapper: ObjectMapper,
) : ReceiptRecognizer {

    override suspend fun recognize(lines: List<ExtractedLine>, catalog: List<Product>): RecognizedReceipt {
        if (lines.isEmpty()) return RecognizedReceipt(emptyList())
        val parts = listOf(mapOf<String, Any>("text" to buildPrompt(lines, catalog)))
        return parse(gemini.generateStructured(parts, SCHEMA, geminiProperties.recognizerApiKey))
    }

    internal fun parse(json: String): RecognizedReceipt {
        val lines: List<RecognizedLineJson> = objectMapper.readValue(json)
        return RecognizedReceipt(
            lines.map { line ->
                RecognizedLine(
                    rawText = line.rawText,
                    action = RecognizedAction.valueOf(line.action),
                    productId = line.productId?.let { ProductId(UUID.fromString(it)) },
                    proposedName = line.proposedName,
                    proposedBrand = line.proposedBrand,
                    quantity = line.quantity,
                    confidence = Confidence.valueOf(line.confidence),
                )
            },
        )
    }

    private fun buildPrompt(lines: List<ExtractedLine>, catalog: List<Product>): String {
        val linesText = lines.joinToString("\n") { "${it.rawText}\t${it.quantity}" }
        val catalogText = catalog.joinToString("\n") { "${it.id.value}\t${it.name}\t${it.brand ?: ""}" }
        return """
            You are given receipt lines (rawText<TAB>quantity) and the pantry catalog
            (id<TAB>name<TAB>brand). Return one object per receipt line, keeping rawText and
            quantity exactly as given. Choose the action:
            - MATCH (with productId): a catalog product is clearly the same item. Understand
              abbreviations and OCR noise — "безлакт ультрапастер 1.8" is the same as
              "молоко без лактозы ультрапастеризованное 1,8%". Match on product identity;
              ignore brand and packaging-size differences.
            - CREATE: a real product that is not in the catalog.
            - UNSURE: not a product (bag, delivery, tip, discount, total).

            For CREATE build a clean canonical name:
            - proposedName: normal readable Russian, abbreviations expanded to full words,
              no brand, no receipt codes or artifacts. Keep the distinguishing attributes
              (type, flavor, fat %, "без лактозы").
            - proposedBrand: the manufacturer brand if visible, otherwise null.

            confidence=HIGH only on an explicit match, otherwise LOW.

            Receipt lines:
            $linesText

            Catalog:
            $catalogText
        """.trimIndent()
    }

    companion object {
        private val SCHEMA = mapOf(
            "type" to "ARRAY",
            "items" to mapOf(
                "type" to "OBJECT",
                "properties" to mapOf(
                    "rawText" to mapOf("type" to "STRING"),
                    "action" to mapOf("type" to "STRING", "enum" to listOf("MATCH", "CREATE", "UNSURE")),
                    "productId" to mapOf("type" to "STRING", "nullable" to true),
                    "proposedName" to mapOf("type" to "STRING", "nullable" to true),
                    "proposedBrand" to mapOf("type" to "STRING", "nullable" to true),
                    "quantity" to mapOf("type" to "INTEGER"),
                    "confidence" to mapOf("type" to "STRING", "enum" to listOf("HIGH", "LOW")),
                ),
                "required" to listOf("rawText", "action", "quantity", "confidence"),
            ),
        )
    }
}

private data class RecognizedLineJson(
    val rawText: String,
    val action: String,
    val productId: String? = null,
    val proposedName: String? = null,
    val proposedBrand: String? = null,
    val quantity: Int,
    val confidence: String,
)
