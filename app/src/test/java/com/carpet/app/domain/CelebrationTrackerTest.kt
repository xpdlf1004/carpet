package com.carpet.app.domain

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CelebrationTrackerTest {

    private val none = Milestone(0, 0)

    @Test
    fun `a real birthday shows once`() {
        val t = CelebrationTracker()
        assertNull(t.observe(Growth(1_990f), demo = false, saved = none))
        assertTrue(t.observe(Growth(2_010f), demo = false, saved = none) is Celebration.Birthday)
        assertNull(t.observe(Growth(2_020f), demo = false, saved = Milestone(1, 0)))
    }

    @Test
    fun `hatching on a demo preset does not block real birthdays`() {
        val t = CelebrationTracker()
        // Hatched while the panel said 50,000 km; the real odometer is 0, saved as such.
        t.startAt(Growth(50_000f), demo = true)
        assertNull(t.observe(Growth(50_000f), demo = true, saved = none))
        // Panel cleared: back to the real 0 km, nothing to show.
        assertNull(t.observe(Growth(0f), demo = false, saved = none))
        // The real first birthday still shows.
        assertTrue(t.observe(Growth(2_001f), demo = false, saved = none) is Celebration.Birthday)
    }

    @Test
    fun `a demo preset above real growth shows the new stage`() {
        val t = CelebrationTracker()
        assertNull(t.observe(Growth(100f), demo = false, saved = none))
        assertTrue(t.observe(Growth(150_000f), demo = true, saved = none) is Celebration.NewStage)
        // Stepping down and up again in the demo shows it again.
        assertNull(t.observe(Growth(0f), demo = true, saved = none))
        assertTrue(t.observe(Growth(150_000f), demo = true, saved = none) is Celebration.NewStage)
    }

    @Test
    fun `nothing right after hatching a used car`() {
        val t = CelebrationTracker()
        t.startAt(Growth(80_000f), demo = false)
        assertNull(t.observe(Growth(80_000f), demo = false, saved = Milestone.of(Growth(80_000f))))
    }
}
