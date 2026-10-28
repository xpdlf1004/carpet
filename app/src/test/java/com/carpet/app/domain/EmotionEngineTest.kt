package com.carpet.app.domain

import com.carpet.app.vehicle.FluidLevel
import com.carpet.app.vehicle.Seat
import com.carpet.app.vehicle.VehicleSignals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmotionEngineTest {

    private val now = 1_000_000L

    private fun decide(s: VehicleSignals, boardedAt: Long? = null) = EmotionEngine.decide(s, boardedAt, now)

    @Test
    fun `no signals is calm`() {
        val mood = decide(VehicleSignals())
        assertEquals(Emotion.CALM, mood.emotion)
        assertNull(mood.suggestion)
    }

    @Test
    fun `battery at threshold is hungry and suggests a charger`() {
        val mood = decide(VehicleSignals(batteryPercent = 20))
        assertEquals(Emotion.HUNGRY, mood.emotion)
        assertEquals(ActionKind.CHARGER, mood.suggestion?.kind)
        assertEquals("배터리 20% 남음", mood.cause)
    }

    @Test
    fun `battery above threshold is not hungry`() {
        assertEquals(Emotion.CALM, decide(VehicleSignals(batteryPercent = 21)).emotion)
    }

    @Test
    fun `starving line differs from hungry line`() {
        val hungry = decide(VehicleSignals(batteryPercent = 15)).line
        val starving = decide(VehicleSignals(batteryPercent = 10)).line
        assertNotEquals(hungry, starving)
    }

    @Test
    fun `low engine oil is sick and suggests a repair shop`() {
        val mood = decide(VehicleSignals(engineOil = FluidLevel.LOW))
        assertEquals(Emotion.SICK, mood.emotion)
        assertEquals(ActionKind.REPAIR_SHOP, mood.suggestion?.kind)
    }

    @Test
    fun `hunger beats low engine oil`() {
        val mood = decide(VehicleSignals(batteryPercent = 10, engineOil = FluidLevel.LOW))
        assertEquals(Emotion.HUNGRY, mood.emotion)
    }

    @Test
    fun `low brake fluid beats hunger`() {
        val mood = decide(VehicleSignals(batteryPercent = 5, brakeFluid = FluidLevel.LOW))
        assertEquals(Emotion.SICK, mood.emotion)
        assertEquals("브레이크액 부족", mood.cause)
    }

    @Test
    fun `critically low engine oil beats hunger`() {
        val mood = decide(VehicleSignals(batteryPercent = 5, engineOil = FluidLevel.CRITICALLY_LOW))
        assertEquals(Emotion.SICK, mood.emotion)
    }

    @Test
    fun `greeting lasts for the greeting window after boarding`() {
        val seated = VehicleSignals(seats = mapOf(Seat.DRIVER to true))
        assertEquals(Emotion.GLAD, decide(seated, boardedAt = now - 1_000).emotion)
        assertEquals(Emotion.CALM, decide(seated, boardedAt = now - EmotionEngine.GREETING_MILLIS - 1).emotion)
    }

    @Test
    fun `greeting mentions companions`() {
        val withKid = VehicleSignals(seats = mapOf(Seat.DRIVER to true, Seat.REAR_LEFT to true))
        val line = decide(withKid, boardedAt = now).line
        assertTrue(line in Lines.greeting(1, LineContext()))
    }

    @Test
    fun `greeting keeps one line for the whole boarding`() {
        val seated = VehicleSignals(seats = mapOf(Seat.DRIVER to true))
        val lines = (0..EmotionEngine.GREETING_MILLIS step 1_000).map {
            EmotionEngine.decide(seated, boardedAtMillis = now, nowMillis = now + it).line
        }
        assertEquals(1, lines.toSet().size)
    }

    @Test
    fun `greeting after days away says it missed you`() {
        val seated = VehicleSignals(seats = mapOf(Seat.DRIVER to true))
        val line = EmotionEngine.decide(seated, now, now, LineContext(daysAway = 5)).line
        assertTrue(line in Lines.greeting(0, LineContext(daysAway = 5)))
    }

    @Test
    fun `calm lines change over time but not every second`() {
        val ctx = LineContext(todayKm = 42f, growth = Growth(1_000f))
        val minute = (0 until 60).map { EmotionEngine.decide(VehicleSignals(), null, it * 1_000L, ctx).line }.toSet()
        assertEquals(1, minute.size)
        val hours = (0 until 24 * 15).map { EmotionEngine.decide(VehicleSignals(), null, it * Lines.ROTATE_MILLIS, ctx).line }.toSet()
        assertTrue(hours.size >= 5)
    }

    @Test
    fun `no line calls the friend a car`() {
        val ctxs = listOf(
            LineContext(), LineContext(hour = 2, dayOfWeek = 1, month = 1, todayKm = 0f),
            LineContext(hour = 8, dayOfWeek = 5, month = 4, todayKm = 120f, growth = Growth(100f)),
            LineContext(hour = 19, dayOfWeek = 6, month = 7, todayKm = 500f, growth = Growth(10_000f)),
            LineContext(hour = 23, dayOfWeek = 7, month = 10, growth = Growth(200_000f), daysAway = 4),
        )
        val all = Lines.starving + Lines.hungry + Lines.brakeFluid + Lines.oilCritical + Lines.powerDown +
            Lines.oilLow + Lines.washer + Lines.rainy +
            ctxs.flatMap { c -> (0..2).flatMap { Lines.greeting(it, c) } + Lines.calm(90, c) }
        all.forEach { assertFalse("「차」 in: $it", it.contains('차')) }
    }

    @Test
    fun `sickness beats greeting`() {
        val mood = decide(VehicleSignals(washerFluidLow = true), boardedAt = now)
        assertEquals(Emotion.SICK, mood.emotion)
    }

    @Test
    fun `wiper on is rainy`() {
        assertEquals(Emotion.RAINY, decide(VehicleSignals(wiperOn = true)).emotion)
    }
}
