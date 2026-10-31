package com.xdd.pantry.infrastructure.telemetry

object PantryTelemetry {

    const val RECEIPT_STAGE_DURATION = "pantry.receipt.stage.duration"
    const val RECEIPT_PIPELINE_DURATION = "pantry.receipt.pipeline.duration"
    const val RECEIPT_STAGE_QUEUE_AGE = "pantry.receipt.stage.queue.age"
    const val RECEIPTS = "pantry.receipts"
    const val RECEIPT_POSITIONS = "pantry.receipt.positions"

    const val RECEIPT_LAST_SUCCESS_TIMESTAMP = "pantry.receipt.last.success.timestamp.seconds"
    const val RECEIPT_LAST_PIPELINE_DURATION = "pantry.receipt.last.pipeline.duration.seconds"
    const val RECEIPT_LAST_STAGE_DURATION = "pantry.receipt.last.stage.duration.seconds"
    const val RECEIPT_LAST_POSITIONS = "pantry.receipt.last.positions"

    const val GEMINI_CALLS = "pantry.gemini.calls"
    const val GEMINI_CALL_DURATION = "pantry.gemini.call.duration"
    const val GEMINI_RETRIES = "pantry.gemini.retries"
    const val GEMINI_TOKENS = "pantry.gemini.tokens"
    const val GEMINI_LAST_CALL_DURATION = "pantry.gemini.last.call.duration.seconds"

    const val TELEGRAM_UPDATES = "pantry.telegram.updates"

    const val QUEUE_DEPTH = "pantry.queue.depth"

    const val AUTH_FAILURES = "pantry.auth.failures"
    const val PANTRIES_TOTAL = "pantry.pantries.total"
    const val STOCK_ITEMS_TOTAL = "pantry.stock.items.total"

    object Tag {
        const val STAGE = "stage"
        const val OUTCOME = "outcome"
        const val OPERATION = "operation"
        const val MODEL = "model"
        const val REASON = "reason"
        const val KIND = "kind"
        const val TYPE = "type"
        const val QUEUE = "queue"
    }

    object Stage {
        const val EXTRACT = "extract"
        const val MATCH = "match"
    }

    object Outcome {
        const val OK = "ok"
        const val REJECTED = "rejected"
        const val CANCELLED = "cancelled"
        const val RATE_LIMITED = "rate_limited"
        const val UNAUTHORIZED = "unauthorized"
        const val FORBIDDEN = "forbidden"
        const val NOT_FOUND = "not_found"
        const val BAD_REQUEST = "bad_request"
        const val UPSTREAM_ERROR = "upstream_error"
        const val ERROR = "error"
    }

    object Operation {
        const val EXTRACT = "extract"
        const val RECOGNIZE = "recognize"
    }

    object RetryReason {
        const val RATE_LIMITED = "rate_limited"
        const val TRANSIENT = "transient"
    }

    object TokenKind {
        const val INPUT = "input"
        const val OUTPUT = "output"
    }

    object UpdateType {
        const val PHOTO = "photo"
        const val START = "start"
        const val OTHER = "other"
    }

    object AuthFailureReason {
        const val MISSING_HEADER = "missing_header"
        const val MALFORMED = "malformed"
        const val MISSING_HASH = "missing_hash"
        const val MISSING_AUTH_DATE = "missing_auth_date"
        const val MISSING_USER = "missing_user"
        const val EXPIRED = "expired"
        const val BAD_SIGNATURE = "bad_signature"
    }

    object Buckets {
        val STAGE = doubleArrayOf(0.25, 0.5, 1.0, 2.5, 5.0, 10.0, 20.0, 30.0, 60.0)
        val PIPELINE = doubleArrayOf(1.0, 2.5, 5.0, 10.0, 20.0, 30.0, 60.0, 120.0, 300.0)
        val QUEUE_AGE = doubleArrayOf(0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0, 30.0, 60.0, 300.0)
    }
}
