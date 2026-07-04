package com.xdd.pantry.application.users

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.users.UserId

interface UserDefaultsRepository {
    fun getDefaultPantryId(userId: UserId): PantryId?
    fun setDefaultPantryId(userId: UserId, pantryId: PantryId)
    fun clearDefaultPantryId(userId: UserId)
}
