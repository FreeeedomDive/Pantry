package com.xdd.pantry.infrastructure.receipts

import tools.jackson.module.kotlin.jacksonObjectMapper
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.receipts.Confidence
import com.xdd.pantry.domain.receipts.RecognizedAction
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.util.UUID

class GeminiReceiptRecognizerParsingTest {

    private val recognizer = GeminiReceiptRecognizer(
        gemini = mockk(),
        geminiProperties = GeminiProperties(recognizerApiKey = "test", extractorApiKey = "test"),
        objectMapper = jacksonObjectMapper(),
    )

    @Test
    fun `parses MATCH, CREATE and UNSURE lines`() {
        val productId = UUID.randomUUID()
        val json = """
            [
              {"rawText":"МОЛОКО ПАСТ 3.2","action":"MATCH","productId":"$productId","quantity":1,"confidence":"HIGH"},
              {"rawText":"ХЛЕБ БОРОДИНСКИЙ","action":"CREATE","proposedName":"Хлеб Бородинский","proposedBrand":null,"quantity":2,"confidence":"LOW"},
              {"rawText":"ПАКЕТ МАЙКА","action":"UNSURE","quantity":1,"confidence":"LOW"}
            ]
        """.trimIndent()

        val result = recognizer.parse(json)

        result.lines shouldHaveSize 3

        val match = result.lines[0]
        match.action shouldBe RecognizedAction.MATCH
        match.productId shouldBe ProductId(productId)
        match.confidence shouldBe Confidence.HIGH

        val create = result.lines[1]
        create.action shouldBe RecognizedAction.CREATE
        create.productId.shouldBeNull()
        create.proposedName shouldBe "Хлеб Бородинский"
        create.quantity shouldBe 2

        val unsure = result.lines[2]
        unsure.action shouldBe RecognizedAction.UNSURE
        unsure.proposedName.shouldBeNull()
    }

    @Test
    fun `returns empty receipt for empty array`() {
        recognizer.parse("[]").lines shouldHaveSize 0
    }
}
