package com.xdd.pantry.application.receipts

import com.xdd.pantry.domain.receipts.ExtractedReceipt
import com.xdd.pantry.domain.receipts.ReceiptImage

interface ReceiptExtractor {
    suspend fun extract(images: List<ReceiptImage>): ExtractedReceipt
}
