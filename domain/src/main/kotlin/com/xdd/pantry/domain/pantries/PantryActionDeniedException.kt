package com.xdd.pantry.domain.pantries

import com.xdd.pantry.domain.exceptions.PantryDomainException
import com.xdd.pantry.domain.users.UserId

class PantryActionDeniedException(val userId: UserId, val pantryId: PantryId): PantryDomainException("User $userId cannot access pantry $pantryId")
