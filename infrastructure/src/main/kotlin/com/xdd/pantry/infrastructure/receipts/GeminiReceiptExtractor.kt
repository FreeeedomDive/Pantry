package com.xdd.pantry.infrastructure.receipts

import com.xdd.pantry.application.receipts.ReceiptExtractor
import com.xdd.pantry.domain.receipts.ExtractedLine
import com.xdd.pantry.domain.receipts.ExtractedReceipt
import com.xdd.pantry.domain.receipts.ReceiptImage
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.Base64

@Component
class GeminiReceiptExtractor(
    private val gemini: GeminiClient,
    private val geminiProperties: GeminiProperties,
    private val objectMapper: ObjectMapper,
) : ReceiptExtractor {

    override suspend fun extract(images: List<ReceiptImage>): ExtractedReceipt {
        if (images.isEmpty()) return ExtractedReceipt(emptyList())
        val parts = images.map { imagePart(it) } + mapOf<String, Any>("text" to PROMPT)
        return parse(gemini.generateStructured(parts, SCHEMA, geminiProperties.extractorApiKey))
    }

    internal fun parse(json: String): ExtractedReceipt {
        val lines: List<ExtractedLineJson> = objectMapper.readValue(json)
        return ExtractedReceipt(lines.map { ExtractedLine(it.rawText, it.quantity) })
    }

    private fun imagePart(image: ReceiptImage): Map<String, Any> = mapOf(
        "inline_data" to mapOf(
            "mime_type" to image.mimeType,
            "data" to Base64.getEncoder().encodeToString(image.bytes),
        ),
    )

    companion object {
        private val PROMPT = """
            You read a photo of a store receipt or a screenshot of a delivery order.
            Extract every purchasable line item as { rawText, quantity }.
            - rawText: the product name as printed, keeping brand and descriptive attributes
              (flavor, fat %, weight, "без лактозы"). Keep the original language and spelling.
              Drop receipt formatting that is not part of the product name: leading item/PLU
              codes, trailing line codes (like ":8" or "%3") and section markers (like "<В>").
            - quantity: the amount, or 1 if not stated.
            Do not classify or match anything. Skip totals, discounts, section headers and payment lines.
        """.trimIndent()

        private val SCHEMA = mapOf(
            "type" to "ARRAY",
            "items" to mapOf(
                "type" to "OBJECT",
                "properties" to mapOf(
                    "rawText" to mapOf("type" to "STRING"),
                    "quantity" to mapOf("type" to "INTEGER"),
                ),
                "required" to listOf("rawText", "quantity"),
            ),
        )
    }
}

private data class ExtractedLineJson(val rawText: String, val quantity: Int)
