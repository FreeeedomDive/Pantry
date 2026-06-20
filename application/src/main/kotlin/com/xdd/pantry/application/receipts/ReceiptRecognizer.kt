package com.xdd.pantry.application.receipts

import com.xdd.pantry.domain.products.Product
import com.xdd.pantry.domain.receipts.ExtractedLine
import com.xdd.pantry.domain.receipts.RecognizedReceipt

interface ReceiptRecognizer {
    suspend fun recognize(lines: List<ExtractedLine>, catalog: List<Product>): RecognizedReceipt
}
