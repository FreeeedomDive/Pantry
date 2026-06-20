package com.xdd.pantry.infrastructure.products

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "stock_items")
class StockItemEntity(
    @Id var id: UUID,
    @Column(name = "product_id", nullable = false) var productId: UUID,
    @Column(name = "quantity", nullable = false) var quantity: Int,
    @Column(name = "purchased_at", nullable = false) var purchasedAt: Instant,
    @Column(name = "expires_at") var expiresAt: LocalDate?,
    @Version var version: Long? = null,
)
