package com.fourdigital.marketintelligence.core.common.ext

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun Instant.isStale(maxAgeMillis: Long): Boolean {
    val age = Clock.System.now().toEpochMilliseconds() - this.toEpochMilliseconds()
    return age > maxAgeMillis
}

fun Instant.ageMillis(): Long =
    Clock.System.now().toEpochMilliseconds() - this.toEpochMilliseconds()

fun Instant.toLocalDateTimeUTC() = this.toLocalDateTime(TimeZone.UTC)

fun Instant.toLocalDateTimeBerlin() = this.toLocalDateTime(TimeZone.of("Europe/Berlin"))

fun Instant.toLocalDateTimeSaoPaulo() = this.toLocalDateTime(TimeZone.of("America/Sao_Paulo"))

fun Instant.toLocalDateTimeNewYork() = this.toLocalDateTime(TimeZone.of("America/New_York"))

fun nowInstant(): Instant = Clock.System.now()
