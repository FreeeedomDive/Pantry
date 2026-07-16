package com.xdd.pantry.bootstrap.web

import com.xdd.pantry.application.pantries.AddPantryMemberUseCase
import com.xdd.pantry.application.pantries.PantryAccessGuard
import com.xdd.pantry.application.users.RegisterUserUseCase
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.users.TelegramUserId
import com.xdd.pantry.domain.users.UserId
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class AddE2ePantryMemberRequest(val telegramUserId: Long)

@Profile("e2e")
@RestController
@RequestMapping("/api/e2e/pantries")
class E2ePantryMemberController(
    private val registerUser: RegisterUserUseCase,
    private val addPantryMember: AddPantryMemberUseCase,
    private val pantryAccessGuard: PantryAccessGuard,
) {
    @PostMapping("/{pantryId}/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun addMember(
        @CurrentUser ownerId: UserId,
        @PathVariable pantryId: UUID,
        @RequestBody request: AddE2ePantryMemberRequest,
    ) {
        val id = PantryId(pantryId)
        pantryAccessGuard.checkAccess(id, ownerId, shouldBeOwner = true)
        val target = registerUser.findOrRegister(TelegramUserId(request.telegramUserId))
        addPantryMember.addPantryMember(id, ownerId, target.id)
    }
}
