package com.xdd.pantry.infrastructure.products

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "products")
class ProductEntity(
    @Id var id: UUID,
    @Column(name = "pantry_id", nullable = false) var pantryId: UUID,
    @Column(name = "name", nullable = false) var name: String,
    @Column(name = "brand") var brand: String?,
    @Column(name = "created_at", nullable = false) var createdAt: Instant,
    @Version var version: Long? = null,
)
