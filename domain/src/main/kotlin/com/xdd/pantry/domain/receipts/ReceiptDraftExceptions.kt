package com.xdd.pantry.domain.receipts

import com.xdd.pantry.domain.exceptions.PantryDomainException

class ReceiptDraftNotFoundException(val draftId: DraftId) :
    PantryDomainException("Receipt draft $draftId not found")

class ReceiptDraftNotReadyException(val draftId: DraftId, val status: DraftStatus) :
    PantryDomainException("Receipt draft $draftId is $status, expected READY")
