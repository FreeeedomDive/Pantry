package com.xdd.pantry.bootstrap.web

import com.xdd.pantry.domain.exceptions.PantryDomainException
import com.xdd.pantry.domain.pantries.CannotDeleteLastPantryException
import com.xdd.pantry.domain.pantries.PantryActionDeniedException
import com.xdd.pantry.domain.pantries.PantryMemberNotFoundException
import com.xdd.pantry.domain.products.ProductNotFoundException
import com.xdd.pantry.domain.receipts.ReceiptDraftNotFoundException
import com.xdd.pantry.domain.receipts.ReceiptDraftNotReadyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ErrorResponse(val message: String)

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(PantryActionDeniedException::class)
    fun denied(e: PantryActionDeniedException) = error(HttpStatus.FORBIDDEN, e)

    @ExceptionHandler(
        ProductNotFoundException::class,
        ReceiptDraftNotFoundException::class,
        PantryMemberNotFoundException::class,
    )
    fun notFound(e: PantryDomainException) = error(HttpStatus.NOT_FOUND, e)

    @ExceptionHandler(ReceiptDraftNotReadyException::class)
    fun conflict(e: ReceiptDraftNotReadyException) = error(HttpStatus.CONFLICT, e)

    @ExceptionHandler(CannotDeleteLastPantryException::class)
    fun conflict(e: CannotDeleteLastPantryException) = error(HttpStatus.CONFLICT, e)

    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(e: IllegalArgumentException) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message ?: "Bad request"))

    private fun error(status: HttpStatus, e: Exception) =
        ResponseEntity.status(status).body(ErrorResponse(e.message ?: status.reasonPhrase))
}
