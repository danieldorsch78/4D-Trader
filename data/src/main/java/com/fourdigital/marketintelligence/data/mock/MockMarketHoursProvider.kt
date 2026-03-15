package com.fourdigital.marketintelligence.data.mock

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.*
import com.fourdigital.marketintelligence.domain.provider.MarketHoursProvider
import kotlinx.datetime.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockMarketHoursProvider @Inject constructor() : MarketHoursProvider {

    override val providerName: String = "MockMarketHours"

    override suspend fun getMarketSession(exchange: Exchange): DataResult<MarketSession> {
        return DataResult.success(buildSession(exchange))
    }

    override suspend fun getAllSessions(): DataResult<Map<Exchange, MarketSession>> {
        val sessions = Exchange.entries.associateWith { buildSession(it) }
        return DataResult.success(sessions)
    }

    override suspend fun isMarketOpen(exchange: Exchange): DataResult<Boolean> {
        val session = buildSession(exchange)
        return DataResult.success(session.status == MarketStatus.OPEN || session.status == MarketStatus.PRE_MARKET)
    }

    private fun buildSession(exchange: Exchange): MarketSession {
        val now = Clock.System.now()
        val tz = TimeZone.of(exchange.timezone)
        val localNow = now.toLocalDateTime(tz)
        val dayOfWeek = localNow.dayOfWeek

        if (exchange == Exchange.CRYPTO_GLOBAL) {
            return MarketSession(
                exchange = exchange,
                status = MarketStatus.OPEN,
                openTime = LocalTime(0, 0),
                closeTime = LocalTime(23, 59, 59),
                is24x7 = true
            )
        }

        val isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
        if (isWeekend) {
            return MarketSession(
                exchange = exchange,
                status = MarketStatus.WEEKEND,
                openTime = getOpenTime(exchange),
                closeTime = getCloseTime(exchange)
            )
        }

        val openTime = getOpenTime(exchange)
        val closeTime = getCloseTime(exchange)
        val currentTime = localNow.time

        val status = when {
            currentTime < openTime -> MarketStatus.PRE_MARKET
            currentTime > closeTime -> MarketStatus.CLOSED
            else -> MarketStatus.OPEN
        }

        return MarketSession(
            exchange = exchange,
            status = status,
            openTime = openTime,
            closeTime = closeTime,
            preMarketOpen = LocalTime(openTime.hour - 1, 0),
            tradingDays = setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            )
        )
    }

    private fun getOpenTime(exchange: Exchange): LocalTime = when (exchange) {
        Exchange.XETRA, Exchange.FRANKFURT -> LocalTime(9, 0)
        Exchange.B3 -> LocalTime(10, 0)
        Exchange.NYSE, Exchange.NASDAQ -> LocalTime(9, 30)
        Exchange.CME -> LocalTime(8, 30)
        Exchange.COMEX, Exchange.NYMEX -> LocalTime(8, 20)
        Exchange.LSE -> LocalTime(8, 0)
        else -> LocalTime(9, 0)
    }

    private fun getCloseTime(exchange: Exchange): LocalTime = when (exchange) {
        Exchange.XETRA, Exchange.FRANKFURT -> LocalTime(17, 30)
        Exchange.B3 -> LocalTime(17, 0)
        Exchange.NYSE, Exchange.NASDAQ -> LocalTime(16, 0)
        Exchange.CME -> LocalTime(15, 0)
        Exchange.COMEX, Exchange.NYMEX -> LocalTime(14, 30)
        Exchange.LSE -> LocalTime(16, 30)
        else -> LocalTime(17, 0)
    }
}
