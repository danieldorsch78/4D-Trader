package com.fourdigital.marketintelligence.feature.alerts.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdigital.marketintelligence.domain.model.*
import com.fourdigital.marketintelligence.domain.repository.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

data class AlertsUiState(
    val rules: List<AlertRule> = emptyList(),
    val events: List<AlertEvent> = emptyList(),
    val showCreateDialog: Boolean = false,
    val selectedTab: Int = 0 // 0 = rules, 1 = history
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertRepo: AlertRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                alertRepo.observeAlerts().collect { rules ->
                    _uiState.update { it.copy(rules = rules) }
                }
            } catch (_: Exception) { /* DB error — keep empty list */ }
        }
        viewModelScope.launch {
            try {
                alertRepo.observeAlertEvents().collect { events ->
                    _uiState.update { it.copy(events = events.sortedByDescending { e -> e.timestamp }) }
                }
            } catch (_: Exception) { /* DB error — keep empty list */ }
        }
    }

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun dismissCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun createAlert(symbol: String, type: AlertType, threshold: Double, message: String) {
        viewModelScope.launch {
            val rule = AlertRule(
                id = "alert_${Clock.System.now().toEpochMilliseconds()}",
                symbol = symbol,
                type = type,
                threshold = threshold,
                message = message,
                createdAt = Clock.System.now()
            )
            alertRepo.createAlert(rule)
            _uiState.update { it.copy(showCreateDialog = false) }
        }
    }

    fun toggleRule(ruleId: String) {
        viewModelScope.launch {
            val rule = _uiState.value.rules.find { it.id == ruleId } ?: return@launch
            alertRepo.toggleAlert(ruleId, !rule.isEnabled)
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            alertRepo.deleteAlert(ruleId)
        }
    }

    fun dismissEvent(eventId: String) {
        viewModelScope.launch {
            alertRepo.dismissEvent(eventId)
        }
    }
}
