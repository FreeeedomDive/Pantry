package com.xdd.pantry.domain.pantries

import com.xdd.pantry.domain.exceptions.PantryDomainException
import com.xdd.pantry.domain.users.UserId

class CannotDeleteLastPantryException(val userId: UserId) :
    PantryDomainException("User $userId cannot delete their last remaining pantry")
