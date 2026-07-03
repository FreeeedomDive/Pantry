package com.xdd.pantry.infrastructure.receipts

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "receipt_drafts")
class ReceiptDraftEntity(
    @Id var id: UUID,
    @Column(name = "pantry_id", nullable = false) var pantryId: UUID,
    @Column(name = "status", nullable = false) var status: String,
    @Column(name = "created_at", nullable = false) var createdAt: Instant,
    @Version var version: Long? = null,
)

@Entity
@Table(name = "receipt_draft_lines")
class ReceiptDraftLineEntity(
    @Id var id: UUID,
    @Column(name = "draft_id", nullable = false) var draftId: UUID,
    @Column(name = "raw_text", nullable = false) var rawText: String,
    @Column(name = "action", nullable = false) var action: String,
    @Column(name = "product_id") var productId: UUID?,
    @Column(name = "proposed_name") var proposedName: String?,
    @Column(name = "proposed_brand") var proposedBrand: String?,
    @Column(name = "quantity", nullable = false) var quantity: Int,
    @Column(name = "confidence", nullable = false) var confidence: String,
    @Column(name = "expires_at") var expiresAt: LocalDate?,
    @Version var version: Long? = null,
)
