package com.xdd.pantry.bootstrap.web

import com.xdd.pantry.application.pantries.CreateNewPantryUseCase
import com.xdd.pantry.application.pantries.CreatePantryInviteUseCase
import com.xdd.pantry.application.pantries.GetPantryMembersUseCase
import com.xdd.pantry.application.pantries.GetUserPantriesUseCase
import com.xdd.pantry.application.pantries.RenamePantryUseCase
import com.xdd.pantry.bootstrap.telegram.InviteLinkBuilder
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/pantries")
class PantryController(
    private val getUserPantries: GetUserPantriesUseCase,
    private val createNewPantry: CreateNewPantryUseCase,
    private val renamePantry: RenamePantryUseCase,
    private val getPantryMembers: GetPantryMembersUseCase,
    private val createPantryInvite: CreatePantryInviteUseCase,
    private val inviteLinks: InviteLinkBuilder,
) {
    @GetMapping
    fun list(@CurrentUser userId: UserId): List<PantryResponse> =
        getUserPantries.getUserPantries(userId).map { it.toResponse() }

    @PostMapping
    fun create(@CurrentUser userId: UserId, @RequestBody request: CreatePantryRequest): PantryResponse =
        createNewPantry.createNewPantry(userId, request.name).toResponse(PantryRole.OWNER)

    @PatchMapping("/{pantryId}")
    fun rename(
        @CurrentUser userId: UserId,
        @PathVariable pantryId: UUID,
        @RequestBody request: RenamePantryRequest,
    ): PantryResponse =
        renamePantry.renamePantry(userId, PantryId(pantryId), request.name).toResponse(PantryRole.OWNER)

    @GetMapping("/{pantryId}/members")
    fun members(@CurrentUser userId: UserId, @PathVariable pantryId: UUID): List<PantryMemberResponse> =
        getPantryMembers.getPantryMembers(userId, PantryId(pantryId)).map { it.toResponse() }

    @PostMapping("/{pantryId}/invites")
    fun createInvite(@CurrentUser userId: UserId, @PathVariable pantryId: UUID): InviteResponse {
        val invite = createPantryInvite.createInvite(userId, PantryId(pantryId))
        return InviteResponse(inviteLinks.buildLink(invite.token), invite.expiresAt)
    }
}
