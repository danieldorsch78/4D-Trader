package com.fourdigital.marketintelligence.data.repository

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.core.database.dao.AlertDao
import com.fourdigital.marketintelligence.core.database.entity.AlertEventEntity
import com.fourdigital.marketintelligence.core.database.entity.AlertRuleEntity
import com.fourdigital.marketintelligence.domain.model.AlertEvent
import com.fourdigital.marketintelligence.domain.model.AlertRule
import com.fourdigital.marketintelligence.domain.model.AlertType
import com.fourdigital.marketintelligence.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val alertDao: AlertDao
) : AlertRepository {

    override fun observeAlerts(): Flow<List<AlertRule>> =
        alertDao.observeRules().map { entities -> entities.map { it.toDomain() } }

    override fun observeAlertEvents(): Flow<List<AlertEvent>> =
        alertDao.observeEvents().map { entities -> entities.map { it.toDomain() } }

    override suspend fun createAlert(rule: AlertRule): DataResult<Unit> {
        alertDao.insertRule(rule.toEntity())
        return DataResult.success(Unit)
    }

    override suspend fun updateAlert(rule: AlertRule): DataResult<Unit> {
        alertDao.updateRule(rule.toEntity())
        return DataResult.success(Unit)
    }

    override suspend fun deleteAlert(id: String): DataResult<Unit> {
        alertDao.deleteRule(id)
        return DataResult.success(Unit)
    }

    override suspend fun toggleAlert(id: String, enabled: Boolean): DataResult<Unit> {
        val entity = alertDao.getRuleById(id) ?: return DataResult.error("Alert rule not found: $id")
        alertDao.updateRule(entity.copy(isEnabled = enabled))
        return DataResult.success(Unit)
    }

    override suspend fun snoozeAlert(id: String, durationMinutes: Int): DataResult<Unit> {
        val entity = alertDao.getRuleById(id) ?: return DataResult.error("Alert rule not found: $id")
        val snoozeUntil = System.currentTimeMillis() + durationMinutes * 60_000L
        alertDao.updateRule(entity.copy(isSnoozed = true, snoozeUntil = snoozeUntil))
        return DataResult.success(Unit)
    }

    override suspend fun recordAlertEvent(event: AlertEvent): DataResult<Unit> {
        alertDao.insertEvent(event.toEntity())
        return DataResult.success(Unit)
    }

    override suspend fun dismissEvent(eventId: String): DataResult<Unit> {
        alertDao.dismissEvent(eventId)
        return DataResult.success(Unit)
    }

    // --- Mapping ---

    private fun AlertRuleEntity.toDomain(): AlertRule = AlertRule(
        id = id,
        symbol = symbol,
        type = try { AlertType.valueOf(type) } catch (_: Exception) { AlertType.PRICE_ABOVE },
        threshold = threshold,
        message = message,
        isEnabled = isEnabled,
        isSnoozed = isSnoozed,
        snoozeUntil = snoozeUntil?.let { Instant.fromEpochMilliseconds(it) },
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        lastTriggeredAt = lastTriggeredAt?.let { Instant.fromEpochMilliseconds(it) },
        triggerCount = triggerCount,
        maxTriggers = maxTriggers
    )

    private fun AlertRule.toEntity(): AlertRuleEntity = AlertRuleEntity(
        id = id,
        symbol = symbol,
        type = type.name,
        threshold = threshold,
        message = message,
        isEnabled = isEnabled,
        isSnoozed = isSnoozed,
        snoozeUntil = snoozeUntil?.toEpochMilliseconds(),
        createdAt = createdAt.toEpochMilliseconds(),
        lastTriggeredAt = lastTriggeredAt?.toEpochMilliseconds(),
        triggerCount = triggerCount,
        maxTriggers = maxTriggers
    )

    private fun AlertEventEntity.toDomain(): AlertEvent = AlertEvent(
        id = id,
        ruleId = ruleId,
        symbol = symbol,
        type = try { AlertType.valueOf(type) } catch (_: Exception) { AlertType.PRICE_ABOVE },
        message = message,
        triggeredValue = triggeredValue,
        timestamp = Instant.fromEpochMilliseconds(timestamp),
        isRead = isRead,
        isDismissed = isDismissed
    )

    private fun AlertEvent.toEntity(): AlertEventEntity = AlertEventEntity(
        id = id,
        ruleId = ruleId,
        symbol = symbol,
        type = type.name,
        message = message,
        triggeredValue = triggeredValue,
        timestamp = timestamp.toEpochMilliseconds(),
        isRead = isRead,
        isDismissed = isDismissed
    )
}
