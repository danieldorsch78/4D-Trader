package com.fourdigital.marketintelligence.feature.watchlist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.Asset
import com.fourdigital.marketintelligence.domain.model.Quote
import com.fourdigital.marketintelligence.domain.model.Watchlist
import com.fourdigital.marketintelligence.domain.repository.QuoteRepository
import com.fourdigital.marketintelligence.domain.repository.SymbolSearchRepository
import com.fourdigital.marketintelligence.domain.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchlistUiState(
    val watchlists: List<Watchlist> = emptyList(),
    val selectedWatchlistId: String = "dax_core",
    val quotes: Map<String, Quote> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Asset> = emptyList(),
    val isSearching: Boolean = false
) {
    val selectedWatchlist: Watchlist?
        get() = watchlists.find { it.id == selectedWatchlistId }
}

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    private val quoteRepository: QuoteRepository,
    private val symbolSearchRepository: SymbolSearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadWatchlists()
    }

    private fun loadWatchlists() {
        viewModelScope.launch {
            try {
                watchlistRepository.observeWatchlists().collect { lists ->
                    _uiState.update { it.copy(watchlists = lists) }
                    refreshCurrentWatchlistQuotes()
                }
            } catch (_: Exception) { /* DB error — keep empty list */ }
        }
    }

    fun selectWatchlist(id: String) {
        _uiState.update { it.copy(selectedWatchlistId = id) }
        viewModelScope.launch { refreshCurrentWatchlistQuotes() }
    }

    fun refresh() {
        viewModelScope.launch { refreshCurrentWatchlistQuotes() }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, searchQuery = "", searchResults = emptyList(), isSearching = false) }
    }

    fun dismissAddDialog() {
        searchJob?.cancel()
        _uiState.update { it.copy(showAddDialog = false, searchQuery = "", searchResults = emptyList(), isSearching = false) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350) // debounce
            _uiState.update { it.copy(isSearching = true) }
            try {
                val results = symbolSearchRepository.search(query)
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }

    fun addSymbol(symbol: String) {
        val wlId = _uiState.value.selectedWatchlistId
        viewModelScope.launch {
            watchlistRepository.addSymbolToWatchlist(wlId, symbol.uppercase().trim())
            _uiState.update { it.copy(showAddDialog = false, searchQuery = "", searchResults = emptyList()) }
            refreshCurrentWatchlistQuotes()
        }
    }

    fun removeSymbol(symbol: String) {
        val wlId = _uiState.value.selectedWatchlistId
        viewModelScope.launch {
            watchlistRepository.removeSymbolFromWatchlist(wlId, symbol)
        }
    }

    fun createWatchlist(name: String) {
        val id = name.lowercase().replace(" ", "_")
        viewModelScope.launch {
            watchlistRepository.createWatchlist(
                Watchlist(id = id, name = name, items = emptyList())
            )
            _uiState.update { it.copy(selectedWatchlistId = id) }
        }
    }

    fun deleteWatchlist(id: String) {
        viewModelScope.launch {
            watchlistRepository.deleteWatchlist(id)
            if (_uiState.value.selectedWatchlistId == id) {
                val remaining = _uiState.value.watchlists.filter { it.id != id }
                _uiState.update {
                    it.copy(selectedWatchlistId = remaining.firstOrNull()?.id ?: "dax_core")
                }
            }
        }
    }

    private suspend fun refreshCurrentWatchlistQuotes() {
        val wl = _uiState.value.selectedWatchlist ?: return
        val symbols = wl.items.map { it.symbol }
        if (symbols.isEmpty()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        when (val result = quoteRepository.refreshQuotes(symbols)) {
            is DataResult.Success -> {
                val quoteMap = result.data.associateBy { it.symbol }
                _uiState.update { it.copy(quotes = quoteMap, isLoading = false, error = null) }
            }
            is DataResult.Error -> {
                _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
            is DataResult.Loading -> {}
        }
    }
}
