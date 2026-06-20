package com.xdd.pantry.infrastructure.receipts

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper

class GeminiReceiptExtractorParsingTest {

    private val extractor = GeminiReceiptExtractor(
        gemini = mockk(),
        objectMapper = jacksonObjectMapper(),
    )

    @Test
    fun `parses raw lines with quantities`() {
        val json = """
            [
              {"rawText":"МОЛОКО ПАСТ 3.2 930МЛ","quantity":1},
              {"rawText":"ЯЙЦО С1 10ШТ","quantity":2}
            ]
        """.trimIndent()

        val result = extractor.parse(json)

        result.lines shouldHaveSize 2
        result.lines[0].rawText shouldBe "МОЛОКО ПАСТ 3.2 930МЛ"
        result.lines[0].quantity shouldBe 1
        result.lines[1].quantity shouldBe 2
    }

    @Test
    fun `returns empty receipt for empty array`() {
        extractor.parse("[]").lines shouldHaveSize 0
    }
}
