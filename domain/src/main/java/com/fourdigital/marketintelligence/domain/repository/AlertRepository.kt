package com.fourdigital.marketintelligence.domain.repository

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.AlertEvent
import com.fourdigital.marketintelligence.domain.model.AlertRule
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    fun observeAlerts(): Flow<List<AlertRule>>
    fun observeAlertEvents(): Flow<List<AlertEvent>>
    suspend fun createAlert(rule: AlertRule): DataResult<Unit>
    suspend fun updateAlert(rule: AlertRule): DataResult<Unit>
    suspend fun deleteAlert(id: String): DataResult<Unit>
    suspend fun toggleAlert(id: String, enabled: Boolean): DataResult<Unit>
    suspend fun snoozeAlert(id: String, durationMinutes: Int): DataResult<Unit>
    suspend fun recordAlertEvent(event: AlertEvent): DataResult<Unit>
    suspend fun dismissEvent(eventId: String): DataResult<Unit>
}
