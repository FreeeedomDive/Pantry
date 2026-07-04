package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.pantries.PantryInvite
import java.util.UUID

interface PantryInviteRepository {
    fun save(invite: PantryInvite): PantryInvite
    fun find(token: UUID): PantryInvite?
}
