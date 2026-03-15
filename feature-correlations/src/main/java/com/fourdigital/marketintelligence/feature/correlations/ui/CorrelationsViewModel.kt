package com.fourdigital.marketintelligence.feature.correlations.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdigital.marketintelligence.analytics.correlation.CorrelationEngine
import com.fourdigital.marketintelligence.analytics.correlation.CorrelationMatrixBuilder
import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.CorrelationSnapshot
import com.fourdigital.marketintelligence.domain.model.CorrelationWindow
import com.fourdigital.marketintelligence.domain.provider.HistoricalDataProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CorrelationsUiState(
    val snapshots: List<CorrelationSnapshot> = emptyList(),
    val selectedWindow: CorrelationWindow = CorrelationWindow.MEDIUM,
    val isLoading: Boolean = true
)

@HiltViewModel
class CorrelationsViewModel @Inject constructor(
    private val matrixBuilder: CorrelationMatrixBuilder,
    private val correlationEngine: CorrelationEngine,
    private val historicalProvider: HistoricalDataProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(CorrelationsUiState())
    val uiState: StateFlow<CorrelationsUiState> = _uiState.asStateFlow()

    init {
        computeCorrelations()
    }

    fun selectWindow(window: CorrelationWindow) {
        _uiState.update { it.copy(selectedWindow = window) }
        computeCorrelations()
    }

    private fun computeCorrelations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val symbols = matrixBuilder.predefinedPairs
                .flatMap { listOf(it.symbolA, it.symbolB) }
                .distinct()
            val window = _uiState.value.selectedWindow
            val days = window.days + 50

            val priceData = mutableMapOf<String, List<Double>>()
            for (symbol in symbols) {
                try {
                    val result = historicalProvider.getDailyBars(symbol, days)
                    when (result) {
                        is DataResult.Success -> {
                            priceData[symbol] = result.data.map { it.close }
                        }
                        else -> {
                            // Use empty list; buildMatrix will skip pairs with insufficient data
                            priceData[symbol] = emptyList()
                        }
                    }
                } catch (_: Exception) {
                    priceData[symbol] = emptyList()
                }
            }

            val snapshots = try {
                matrixBuilder.buildMatrix(priceData, window)
            } catch (_: Exception) {
                emptyList()
            }
            _uiState.update { it.copy(snapshots = snapshots, isLoading = false) }
        }
    }
}
