package com.fourdigital.marketintelligence.data.repository

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.core.database.dao.WatchlistDao
import com.fourdigital.marketintelligence.core.database.entity.WatchlistEntity
import com.fourdigital.marketintelligence.core.database.entity.WatchlistItemEntity
import com.fourdigital.marketintelligence.data.mock.DemoAssets
import com.fourdigital.marketintelligence.domain.model.*
import com.fourdigital.marketintelligence.domain.repository.WatchlistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepositoryImpl @Inject constructor(
    private val watchlistDao: WatchlistDao
) : WatchlistRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Seed default watchlists on first launch and backfill missing defaults on updates.
        scope.launch {
            try {
                syncDefaultWatchlists()
            } catch (e: Exception) {
                // Silently handle seed failure - non-critical
            }
        }
    }

    override fun observeWatchlists(): Flow<List<Watchlist>> {
        return watchlistDao.observeAll().map { entities ->
            entities.map { entity ->
                val items = watchlistDao.getItems(entity.id)
                entity.toDomain(items)
            }
        }
    }

    override fun observeWatchlist(id: String): Flow<Watchlist?> {
        return watchlistDao.observeById(id).map { entity ->
            entity?.let {
                val items = watchlistDao.getItems(it.id)
                it.toDomain(items)
            }
        }
    }

    override suspend fun getWatchlist(id: String): DataResult<Watchlist> {
        val entity = watchlistDao.getById(id)
            ?: return DataResult.error("Watchlist not found: $id")
        val items = watchlistDao.getItems(id)
        return DataResult.success(entity.toDomain(items))
    }

    override suspend fun createWatchlist(watchlist: Watchlist): DataResult<Unit> {
        watchlistDao.insert(watchlist.toEntity())
        watchlist.items.forEach { item ->
            watchlistDao.insertItem(item.toEntity(watchlist.id))
        }
        return DataResult.success(Unit)
    }

    override suspend fun updateWatchlist(watchlist: Watchlist): DataResult<Unit> {
        watchlistDao.update(watchlist.toEntity())
        return DataResult.success(Unit)
    }

    override suspend fun deleteWatchlist(id: String): DataResult<Unit> {
        watchlistDao.deleteAllItems(id)
        watchlistDao.delete(id)
        return DataResult.success(Unit)
    }

    override suspend fun addSymbolToWatchlist(watchlistId: String, symbol: String): DataResult<Unit> {
        val asset = DemoAssets.assetBySymbol(symbol) ?: Asset(
            symbol = symbol,
            name = symbol,
            assetClass = AssetClass.UNKNOWN,
            exchange = Exchange.UNKNOWN,
            currency = "USD"
        )
        val currentItems = watchlistDao.getItems(watchlistId)
        if (currentItems.any { it.symbol == symbol }) {
            return DataResult.error("$symbol already in watchlist")
        }
        watchlistDao.insertItem(
            WatchlistItemEntity(
                watchlistId = watchlistId,
                symbol = symbol,
                assetName = asset.name,
                assetClass = asset.assetClass.name,
                exchange = asset.exchange.name,
                currency = asset.currency,
                isin = asset.isin,
                sortOrder = currentItems.size,
                addedTimestamp = Clock.System.now().toEpochMilliseconds()
            )
        )
        return DataResult.success(Unit)
    }

    override suspend fun removeSymbolFromWatchlist(watchlistId: String, symbol: String): DataResult<Unit> {
        watchlistDao.deleteItem(watchlistId, symbol)
        return DataResult.success(Unit)
    }

    override suspend fun getDefaultWatchlists(): List<Watchlist> {
        return DemoAssets.defaultWatchlists
    }

    private suspend fun syncDefaultWatchlists() {
        DemoAssets.defaultWatchlists.forEach { defaultWatchlist ->
            val existing = watchlistDao.getById(defaultWatchlist.id)
            if (existing == null) {
                watchlistDao.insert(defaultWatchlist.toEntity())
                defaultWatchlist.items.forEach { item ->
                    watchlistDao.insertItem(item.toEntity(defaultWatchlist.id))
                }
            }
        }
    }

    // --- Mapping ---

    private fun WatchlistEntity.toDomain(items: List<WatchlistItemEntity>): Watchlist = Watchlist(
        id = id,
        name = name,
        description = description,
        items = items.map { it.toDomain() },
        sortOrder = sortOrder,
        isDefault = isDefault
    )

    private fun WatchlistItemEntity.toDomain(): WatchlistItem = WatchlistItem(
        symbol = symbol,
        asset = Asset(
            symbol = symbol,
            name = assetName,
            assetClass = try { AssetClass.valueOf(assetClass) } catch (_: Exception) { AssetClass.UNKNOWN },
            exchange = try { Exchange.valueOf(exchange) } catch (_: Exception) { Exchange.UNKNOWN },
            currency = currency,
            isin = isin
        ),
        sortOrder = sortOrder,
        notes = notes,
        addedTimestamp = addedTimestamp
    )

    private fun Watchlist.toEntity(): WatchlistEntity = WatchlistEntity(
        id = id,
        name = name,
        description = description,
        sortOrder = sortOrder,
        isDefault = isDefault
    )

    private fun WatchlistItem.toEntity(watchlistId: String): WatchlistItemEntity = WatchlistItemEntity(
        watchlistId = watchlistId,
        symbol = symbol,
        assetName = asset.name,
        assetClass = asset.assetClass.name,
        exchange = asset.exchange.name,
        currency = asset.currency,
        isin = asset.isin,
        sortOrder = sortOrder,
        notes = notes,
        addedTimestamp = addedTimestamp
    )
}
