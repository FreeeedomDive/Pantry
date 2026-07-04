package com.xdd.pantry.application.receipts

import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.application.users.UserRepository
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.receipts.DraftId
import com.xdd.pantry.domain.receipts.DraftStatus
import com.xdd.pantry.domain.receipts.ReceiptDraftNotFoundException
import com.xdd.pantry.domain.receipts.ReceiptDraftNotReadyException
import com.xdd.pantry.domain.users.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MoveReceiptDraftUseCase(
    private val drafts: ReceiptDraftRepository,
    private val accessGuard: PantryAccessGuard,
    private val users: UserRepository,
    private val matchingQueue: ReceiptMatchingQueue,
) {
    fun move(userId: UserId, currentPantryId: PantryId, draftId: DraftId, targetPantryId: PantryId) {
        accessGuard.checkAccess(currentPantryId, userId)
        accessGuard.checkAccess(targetPantryId, userId)
        val draft = drafts.getDraft(draftId) ?: throw ReceiptDraftNotFoundException(draftId)
        if (draft.pantryId != currentPantryId) throw ReceiptDraftNotFoundException(draftId)
        if (draft.status != DraftStatus.READY) throw ReceiptDraftNotReadyException(draftId, draft.status)

        val user = users.findById(userId) ?: throw ReceiptDraftNotFoundException(draftId)
        drafts.updateStatus(draftId, DraftStatus.MATCHING)
        matchingQueue.requestMatching(draftId, targetPantryId, user.telegramUserId, user.telegramUserId.value)
    }
}
