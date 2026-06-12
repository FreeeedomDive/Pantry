package com.xdd.pantry.domain.pantries

import com.xdd.pantry.domain.users.UserId
import java.time.Instant

enum class PantryRole { OWNER, MEMBER }

data class PantryMember(val pantryId: PantryId, val userId: UserId, val role: PantryRole, val joinedAt: Instant)
