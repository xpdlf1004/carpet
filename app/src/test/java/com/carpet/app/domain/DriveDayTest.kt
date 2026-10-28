package com.carpet.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DriveDayTest {

    @Test
    fun `today counts from where yesterday ended`() {
        var log = DriveDay.accept(DriveLog(), 1_000f, today = 10)
        assertEquals(0f, DriveDay.todayKm(log, 1_000f, 10)!!, 0.001f)
        log = DriveDay.accept(log, 1_030f, today = 10)
        assertEquals(30f, DriveDay.todayKm(log, 1_030f, 10)!!, 0.001f)
        // Next morning the app comes up after 12 km were already driven.
        log = DriveDay.accept(log, 1_042f, today = 11)
        assertEquals(12f, DriveDay.todayKm(log, 1_042f, 11)!!, 0.001f)
    }

    @Test
    fun `days away counts from the last day we moved`() {
        var log = DriveDay.accept(DriveLog(), 500f, today = 1)
        log = DriveDay.accept(log, 520f, today = 1)
        assertNull(DriveDay.daysAway(log, 1))
        assertEquals(4, DriveDay.daysAway(log, 5))
        // Sitting in a parked car does not count as a drive.
        log = DriveDay.accept(log, 520f, today = 5)
        assertEquals(4, DriveDay.daysAway(log, 5))
    }

    @Test
    fun `a lower reading restarts the day instead of going negative`() {
        var log = DriveDay.accept(DriveLog(), 80_000f, today = 3)
        log = DriveDay.accept(log, 0f, today = 3)
        assertEquals(0f, DriveDay.todayKm(log, 0f, 3)!!, 0.001f)
    }

    @Test
    fun `a jump bigger than a stretch of driving is not today's drive`() {
        var log = DriveDay.accept(DriveLog(), 0f, today = 7)
        log = DriveDay.accept(log, 40f, today = 7)
        log = DriveDay.accept(log, 150_040f, today = 7)
        assertEquals(40f, DriveDay.todayKm(log, 150_040f, 7)!!, 0.001f)
        log = DriveDay.accept(log, 150_050f, today = 7)
        assertEquals(50f, DriveDay.todayKm(log, 150_050f, 7)!!, 0.001f)
    }
}
