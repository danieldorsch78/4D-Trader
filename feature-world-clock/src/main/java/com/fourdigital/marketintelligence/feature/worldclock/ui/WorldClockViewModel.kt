package com.fourdigital.marketintelligence.feature.worldclock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.MarketSession
import com.fourdigital.marketintelligence.domain.provider.MarketHoursProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import javax.inject.Inject

data class WorldClockUiState(
    val sessions: List<MarketSession> = emptyList(),
    val berlinTime: String = "",
    val saoPauloTime: String = "",
    val newYorkTime: String = "",
    val utcTime: String = "",
    val berlinDate: String = "",
    val saoPauloDate: String = "",
    val newYorkDate: String = "",
    val utcDate: String = ""
)

@HiltViewModel
class WorldClockViewModel @Inject constructor(
    private val marketHoursProvider: MarketHoursProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorldClockUiState())
    val uiState: StateFlow<WorldClockUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
        startClockTicker()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            val result = marketHoursProvider.getAllSessions()
            when (result) {
                is DataResult.Success -> {
                    _uiState.update { it.copy(sessions = result.data.values.toList()) }
                }
                else -> {}
            }
        }
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            while (isActive) {
                updateClocks()
                delay(1000L)
            }
        }
    }

    private fun updateClocks() {
        val now = Clock.System.now()

        val berlin = now.toLocalDateTime(TimeZone.of("Europe/Berlin"))
        val saoPaulo = now.toLocalDateTime(TimeZone.of("America/Sao_Paulo"))
        val newYork = now.toLocalDateTime(TimeZone.of("America/New_York"))
        val utc = now.toLocalDateTime(TimeZone.UTC)

        _uiState.update {
            it.copy(
                berlinTime = formatTime(berlin),
                saoPauloTime = formatTime(saoPaulo),
                newYorkTime = formatTime(newYork),
                utcTime = formatTime(utc),
                berlinDate = formatDate(berlin),
                saoPauloDate = formatDate(saoPaulo),
                newYorkDate = formatDate(newYork),
                utcDate = formatDate(utc)
            )
        }
    }

    private fun formatTime(dt: LocalDateTime): String {
        return "%02d:%02d:%02d".format(dt.hour, dt.minute, dt.second)
    }

    private fun formatDate(dt: LocalDateTime): String {
        val dayName = dt.dayOfWeek.name.take(3)
        return "$dayName ${"%02d".format(dt.dayOfMonth)}.${"%02d".format(dt.monthNumber)}.${dt.year}"
    }
}
