package com.xdd.pantry.bootstrap.web

import com.xdd.pantry.application.pantries.CreateNewPantryUseCase
import com.xdd.pantry.application.pantries.CreatePantryInviteUseCase
import com.xdd.pantry.application.pantries.GetPantryMembersUseCase
import com.xdd.pantry.application.pantries.GetUserPantriesUseCase
import com.xdd.pantry.application.pantries.KickPantryMemberUseCase
import com.xdd.pantry.application.pantries.LeaveOrDeletePantryUseCase
import com.xdd.pantry.application.pantries.RenamePantryUseCase
import com.xdd.pantry.application.pantries.SetDefaultPantryUseCase
import com.xdd.pantry.application.users.UserDefaultsRepository
import com.xdd.pantry.bootstrap.telegram.InviteLinkBuilder
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.pantries.PantryRole
import com.xdd.pantry.domain.users.UserId
import org.springframework.web.bind.annotation.DeleteMapping
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
    private val setDefaultPantry: SetDefaultPantryUseCase,
    private val leaveOrDeletePantry: LeaveOrDeletePantryUseCase,
    private val getPantryMembers: GetPantryMembersUseCase,
    private val kickPantryMember: KickPantryMemberUseCase,
    private val createPantryInvite: CreatePantryInviteUseCase,
    private val inviteLinks: InviteLinkBuilder,
    private val userDefaults: UserDefaultsRepository,
) {
    @GetMapping
    fun list(@CurrentUser userId: UserId): List<PantryResponse> =
        getUserPantries.getUserPantries(userId).map { it.toResponse() }

    @PostMapping
    fun create(@CurrentUser userId: UserId, @RequestBody request: CreatePantryRequest): PantryResponse {
        val pantry = createNewPantry.createNewPantry(userId, request.name)
        return pantry.toResponse(PantryRole.OWNER, isDefault(userId, pantry.id))
    }

    @PatchMapping("/{pantryId}")
    fun rename(
        @CurrentUser userId: UserId,
        @PathVariable pantryId: UUID,
        @RequestBody request: RenamePantryRequest,
    ): PantryResponse {
        val pantry = renamePantry.renamePantry(userId, PantryId(pantryId), request.name)
        return pantry.toResponse(PantryRole.OWNER, isDefault(userId, pantry.id))
    }

    @PostMapping("/{pantryId}/default")
    fun setDefault(@CurrentUser userId: UserId, @PathVariable pantryId: UUID) {
        setDefaultPantry.setDefault(userId, PantryId(pantryId))
    }

    @DeleteMapping("/{pantryId}")
    fun leaveOrDelete(@CurrentUser userId: UserId, @PathVariable pantryId: UUID) {
        leaveOrDeletePantry.leaveOrDelete(userId, PantryId(pantryId))
    }

    @GetMapping("/{pantryId}/members")
    fun members(@CurrentUser userId: UserId, @PathVariable pantryId: UUID): List<PantryMemberResponse> =
        getPantryMembers.getPantryMembers(userId, PantryId(pantryId)).map { it.toResponse() }

    @DeleteMapping("/{pantryId}/members/{userId}")
    fun kickMember(
        @CurrentUser ownerId: UserId,
        @PathVariable pantryId: UUID,
        @PathVariable userId: UUID,
    ) {
        kickPantryMember.kickPantryMember(PantryId(pantryId), ownerId, UserId(userId))
    }

    @PostMapping("/{pantryId}/invites")
    fun createInvite(@CurrentUser userId: UserId, @PathVariable pantryId: UUID): InviteResponse {
        val invite = createPantryInvite.createInvite(userId, PantryId(pantryId))
        return InviteResponse(inviteLinks.buildLink(invite.token), invite.expiresAt)
    }

    private fun isDefault(userId: UserId, pantryId: PantryId): Boolean =
        userDefaults.getDefaultPantryId(userId) == pantryId
}
