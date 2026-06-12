package com.xdd.pantry.application.pantries

import com.xdd.pantry.domain.users.UserRegistered
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class CreateDefaultPantryOnRegistration(
    private val createNewPantry: CreateNewPantryUseCase,
) {
    @EventListener
    fun on(event: UserRegistered) {
        createNewPantry.createNewPantry(event.userId, DEFAULT_PANTRY_NAME)
    }

    companion object {
        const val DEFAULT_PANTRY_NAME = "Default"
    }
}
