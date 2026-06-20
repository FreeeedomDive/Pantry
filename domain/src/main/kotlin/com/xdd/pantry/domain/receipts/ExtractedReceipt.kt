package com.xdd.pantry.domain.receipts

data class ExtractedLine(val rawText: String, val quantity: Int)

data class ExtractedReceipt(val lines: List<ExtractedLine>)
