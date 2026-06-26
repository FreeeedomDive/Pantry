package com.xdd.pantry.infrastructure.products

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.springframework.data.domain.Persistable
import java.io.Serializable
import java.util.UUID

@Embeddable
data class ProductAliasKey(
    @Column(name = "pantry_id") var pantryId: UUID,
    @Column(name = "alias") var alias: String,
) : Serializable

@Entity
@Table(name = "product_aliases")
class ProductAliasEntity(
    @EmbeddedId var key: ProductAliasKey,
    @Column(name = "product_id", nullable = false) var productId: UUID,
) : Persistable<ProductAliasKey> {

    @Transient
    private var isNew: Boolean = true

    override fun getId(): ProductAliasKey = key
    override fun isNew(): Boolean = isNew

    @PostLoad
    @PostPersist
    fun markNotNew() {
        isNew = false
    }
}
