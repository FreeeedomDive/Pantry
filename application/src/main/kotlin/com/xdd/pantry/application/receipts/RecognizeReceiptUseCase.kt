package com.xdd.pantry.application.receipts

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.application.products.ProductAliasRepository
import com.xdd.pantry.application.products.ProductRepository
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.normalizeAlias
import com.xdd.pantry.domain.receipts.Confidence
import com.xdd.pantry.domain.receipts.ExtractedLine
import com.xdd.pantry.domain.receipts.ReceiptImage
import com.xdd.pantry.domain.receipts.RecognizedAction
import com.xdd.pantry.domain.receipts.RecognizedLine
import com.xdd.pantry.domain.receipts.RecognizedReceipt
import com.xdd.pantry.domain.users.UserId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

@Service
class RecognizeReceiptUseCase(
    private val extractor: ReceiptExtractor,
    private val recognizer: ReceiptRecognizer,
    private val products: ProductRepository,
    private val aliases: ProductAliasRepository,
    private val accessGuard: PantryAccessGuard,
) {
    suspend fun recognizeReceipt(
        userId: UserId,
        pantryId: PantryId,
        images: List<ReceiptImage>,
    ): RecognizedReceipt {
        withContext(Dispatchers.IO) { accessGuard.checkAccess(pantryId, userId) }

        val extracted = extractor.extract(images)
        if (extracted.lines.isEmpty()) return RecognizedReceipt(emptyList())

        val (aliasMap, catalog) = withContext(Dispatchers.IO) {
            val aliasMap = aliases.getPantryAliases(pantryId).associate { it.alias to it.productId }
            aliasMap to products.getPantryProducts(pantryId)
        }

        val matchedLocally = mutableListOf<RecognizedLine>()
        val leftovers = mutableListOf<ExtractedLine>()
        extracted.lines.forEach { line ->
            val productId = aliasMap[normalizeAlias(line.rawText)]
            if (productId != null) {
                matchedLocally += RecognizedLine(
                    rawText = line.rawText,
                    action = RecognizedAction.MATCH,
                    productId = productId,
                    proposedName = null,
                    proposedBrand = null,
                    quantity = line.quantity,
                    confidence = Confidence.HIGH,
                )
            } else {
                leftovers += line
            }
        }

        val recognizedLeftovers =
            if (leftovers.isEmpty()) emptyList()
            else recognizer.recognize(leftovers, catalog).lines

        return RecognizedReceipt(matchedLocally + recognizedLeftovers)
    }
}
