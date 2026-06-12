package com.xdd.pantry.domain.pantries

import java.time.Instant
import java.util.UUID

@JvmInline value class PantryId(val value: UUID)

data class Pantry(val id: PantryId, val name: String, val createdAt: Instant)
