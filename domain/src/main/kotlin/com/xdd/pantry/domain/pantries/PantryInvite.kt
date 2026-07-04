package com.xdd.pantry.domain.pantries

import com.xdd.pantry.domain.users.UserId
import java.time.Instant
import java.util.UUID

data class PantryInvite(
    val token: UUID,
    val pantryId: PantryId,
    val createdBy: UserId,
    val createdAt: Instant,
    val expiresAt: Instant,
) {
    fun isExpired(now: Instant): Boolean = now.isAfter(expiresAt)
}
