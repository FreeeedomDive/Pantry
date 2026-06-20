package com.xdd.pantry.infrastructure.products

import com.xdd.pantry.application.products.StockRepository
import com.xdd.pantry.domain.pantries.PantryId
import com.xdd.pantry.domain.products.ProductId
import com.xdd.pantry.domain.products.Quantity
import com.xdd.pantry.domain.products.StockItem
import com.xdd.pantry.domain.products.StockItemId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

interface StockItemJpaRepository : JpaRepository<StockItemEntity, UUID> {
    fun findByProductId(productId: UUID): List<StockItemEntity>

    @Query(
        """
        select s from StockItemEntity s
        where s.productId in (select p.id from ProductEntity p where p.pantryId = :pantryId)
        """
    )
    fun findByPantryId(@Param("pantryId") pantryId: UUID): List<StockItemEntity>
}

@Repository
class StockRepositoryAdapter(
    private val stock: StockItemJpaRepository,
) : StockRepository {

    override fun save(stockItem: StockItem): StockItem = stock.save(stockItem.toEntity()).toDomain()

    override fun getStockItem(stockItemId: StockItemId): StockItem? =
        stock.findById(stockItemId.value).map { it.toDomain() }.orElse(null)

    override fun getProductStock(productId: ProductId): List<StockItem> =
        stock.findByProductId(productId.value).map { it.toDomain() }

    override fun getPantryStock(pantryId: PantryId): List<StockItem> =
        stock.findByPantryId(pantryId.value).map { it.toDomain() }

    override fun delete(stockItemId: StockItemId) = stock.deleteById(stockItemId.value)
}

private fun StockItemEntity.toDomain() =
    StockItem(StockItemId(id), ProductId(productId), Quantity(quantity), purchasedAt, expiresAt)

private fun StockItem.toEntity() =
    StockItemEntity(id.value, productId.value, quantity.value, purchasedAt, expiresAt)
