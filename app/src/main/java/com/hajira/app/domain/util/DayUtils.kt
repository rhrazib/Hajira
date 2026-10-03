package com.hajira.app.domain.util

import java.time.Clock
import java.time.Instant
import java.time.LocalDate

fun isToday(epochMillis: Long, clock: Clock): Boolean =
    Instant.ofEpochMilli(epochMillis).atZone(clock.zone).toLocalDate() == LocalDate.now(clock)
