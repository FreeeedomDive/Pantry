package com.xdd.pantry.domain.pantries

import com.xdd.pantry.domain.exceptions.PantryDomainException
import com.xdd.pantry.domain.users.UserId

class PantryMemberNotFoundException(val pantryId: PantryId, val memberId: UserId): PantryDomainException("Pantry member $memberId not found in pantry $pantryId")
