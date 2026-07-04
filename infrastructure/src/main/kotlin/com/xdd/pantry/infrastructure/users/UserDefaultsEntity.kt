package com.xdd.pantry.infrastructure.users

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID

@Entity
@Table(name = "user_defaults")
class UserDefaultsEntity(
    @Id @Column(name = "user_id") var userId: UUID,
    @Column(name = "default_pantry_id") var defaultPantryId: UUID?,
    @Version var version: Long? = null,
)
