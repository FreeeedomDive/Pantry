package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.pantries.Pantry
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryMember
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId

interface PantryRepository {
    fun getPantry(pantryId: PantryId): Pantry?
    fun getUserPantries(userId: UserId): List<Pantry>
    fun getPantryMember(pantryId: PantryId, userId: UserId): PantryMember?
    fun getPantryMembers(pantryId: PantryId): List<PantryMember>
    fun save(pantry: Pantry): Pantry
    fun save(newMember: PantryMember): PantryMember
    fun updateRole(pantryId: PantryId, userId: UserId, newRole: PantryRole)
    fun delete(pantryId: PantryId)
    fun deletePantryMember(pantryId: PantryId, userId: UserId)
}