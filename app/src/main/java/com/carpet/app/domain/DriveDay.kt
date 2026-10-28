package com.carpet.app.domain

/** Where today's drive started and the last day we drove, for 「오늘 N km」 and 「오랜만이야」. */
data class DriveLog(
    /** Epoch day the log is for. */
    val dayEpoch: Long? = null,
    val dayStartKm: Float? = null,
    val lastDriveEpoch: Long? = null,
    val lastOdometerKm: Float? = null,
)

object DriveDay {

    /**
     * Readings arrive every few seconds while driving. A bigger step between two readings means
     * the app missed a long stretch (or the value was set by hand), so it is not counted as today.
     */
    const val MAX_STEP_KM = 300f

    /** The log after a real odometer reading on [today] (epoch day). */
    fun accept(log: DriveLog, readingKm: Float, today: Long): DriveLog {
        val last = log.lastOdometerKm
        val step = if (last != null) readingKm - last else 0f
        val plausible = step in 0f..MAX_STEP_KM
        // A new day starts where the last reading left off, so km driven before the app came up still count.
        val start = when {
            log.dayEpoch != today -> if (last != null && plausible) last else readingKm
            log.dayStartKm == null || log.dayStartKm > readingKm -> readingKm
            !plausible -> log.dayStartKm + step
            else -> log.dayStartKm
        }
        val moved = last != null && readingKm - last >= 0.1f
        return DriveLog(
            dayEpoch = today,
            dayStartKm = start,
            lastDriveEpoch = if (moved) today else log.lastDriveEpoch,
            lastOdometerKm = readingKm,
        )
    }

    fun todayKm(log: DriveLog, readingKm: Float?, today: Long): Float? {
        if (readingKm == null || log.dayEpoch != today) return null
        val start = log.dayStartKm ?: return null
        return (readingKm - start).coerceAtLeast(0f)
    }

    /** Days since the last day we drove, or null when we drove today or never. */
    fun daysAway(log: DriveLog, today: Long): Int? {
        val last = log.lastDriveEpoch ?: return null
        return (today - last).toInt().takeIf { it > 0 }
    }
}
