package com.xdd.pantry.bootstrap.web

import com.xdd.pantry.application.pantries.CreateNewPantryUseCase
import com.xdd.pantry.application.pantries.GetUserPantriesUseCase
import com.xdd.pantry.domain.users.UserId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/pantries")
class PantryController(
    private val getUserPantries: GetUserPantriesUseCase,
    private val createNewPantry: CreateNewPantryUseCase,
) {
    @GetMapping
    fun list(@CurrentUser userId: UserId): List<PantryResponse> =
        getUserPantries.getUserPantries(userId).map { it.toResponse() }

    @PostMapping
    fun create(@CurrentUser userId: UserId, @RequestBody request: CreatePantryRequest): PantryResponse =
        createNewPantry.createNewPantry(userId, request.name).toResponse()
}
