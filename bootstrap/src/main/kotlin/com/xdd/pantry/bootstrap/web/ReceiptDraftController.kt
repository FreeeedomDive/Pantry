package com.xdd.pantry.bootstrap.web

import com.xdd.pantry.application.receipts.ConfirmReceiptDraftUseCase
import com.xdd.pantry.application.receipts.GetReceiptDraftUseCase
import com.xdd.pantry.application.receipts.UpdateReceiptDraftUseCase
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.receipts.DraftId
import com.xdd.pantry.domain.users.UserId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/pantries/{pantryId}/drafts")
class ReceiptDraftController(
    private val getReceiptDraft: GetReceiptDraftUseCase,
    private val updateReceiptDraft: UpdateReceiptDraftUseCase,
    private val confirmReceiptDraft: ConfirmReceiptDraftUseCase,
) {
    @GetMapping("/{draftId}")
    fun get(
        @CurrentUser userId: UserId,
        @PathVariable pantryId: UUID,
        @PathVariable draftId: UUID,
    ): DraftResponse =
        getReceiptDraft.getDraft(userId, PantryId(pantryId), DraftId(draftId)).toResponse()

    @PutMapping("/{draftId}")
    fun update(
        @CurrentUser userId: UserId,
        @PathVariable pantryId: UUID,
        @PathVariable draftId: UUID,
        @RequestBody request: UpdateDraftRequest,
    ): DraftResponse =
        updateReceiptDraft.updateDraft(userId, PantryId(pantryId), DraftId(draftId), request.toRecognizedReceipt()).toResponse()

    @PostMapping("/{draftId}/confirm")
    fun confirm(@CurrentUser userId: UserId, @PathVariable pantryId: UUID, @PathVariable draftId: UUID) {
        confirmReceiptDraft.confirmDraft(userId, PantryId(pantryId), DraftId(draftId))
    }
}
