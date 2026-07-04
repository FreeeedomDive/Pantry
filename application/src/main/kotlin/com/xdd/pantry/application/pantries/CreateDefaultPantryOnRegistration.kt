package com.xdd.pantry.application.pantries

import com.xdd.pantry.application.users.UserDefaultsRepository
import com.xdd.pantry.domain.users.UserRegistered
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class CreateDefaultPantryOnRegistration(
    private val createNewPantry: CreateNewPantryUseCase,
    private val userDefaults: UserDefaultsRepository,
) {
    @EventListener
    fun on(event: UserRegistered) {
        val pantry = createNewPantry.createNewPantry(event.userId, DEFAULT_PANTRY_NAME)
        userDefaults.setDefaultPantryId(event.userId, pantry.id)
    }

    companion object {
        const val DEFAULT_PANTRY_NAME = "Default"
    }
}
