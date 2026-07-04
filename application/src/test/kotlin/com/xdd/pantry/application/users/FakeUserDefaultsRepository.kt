package com.xdd.pantry.application.users

import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.users.UserId

class FakeUserDefaultsRepository : UserDefaultsRepository {
    private val defaults = mutableMapOf<UserId, PantryId?>()

    override fun getDefaultPantryId(userId: UserId): PantryId? = defaults[userId]

    override fun setDefaultPantryId(userId: UserId, pantryId: PantryId) {
        defaults[userId] = pantryId
    }

    override fun clearDefaultPantryId(userId: UserId) {
        defaults[userId] = null
    }
}
