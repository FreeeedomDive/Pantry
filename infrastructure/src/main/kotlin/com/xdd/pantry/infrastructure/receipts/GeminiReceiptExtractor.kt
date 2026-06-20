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
    private val objectMapper: ObjectMapper,
) : ReceiptExtractor {

    override suspend fun extract(images: List<ReceiptImage>): ExtractedReceipt {
        if (images.isEmpty()) return ExtractedReceipt(emptyList())
        val parts = images.map { imagePart(it) } + mapOf<String, Any>("text" to PROMPT)
        return parse(gemini.generateStructured(parts, SCHEMA))
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
            Extract EVERY line item: rawText is the item text exactly as written in the source
            (keep the original language and spelling), quantity is the amount (use 1 if not stated).
            Do not classify or match anything — just transcribe the items.
            Skip totals, section headers, and non-item receipt formatting lines.
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
