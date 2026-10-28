package com.carpet.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SpeciesTest {

    @Test
    fun `weights add up to 100 so shown odds are exact`() {
        assertEquals(100, Species.TOTAL_WEIGHT)
        assertEquals(100, Species.entries.sumOf { it.chancePercent })
    }

    @Test
    fun `roll boundaries map to the right species`() {
        assertEquals(Species.FOX, Species.forRoll(0))
        assertEquals(Species.FOX, Species.forRoll(34))
        assertEquals(Species.TURTLE, Species.forRoll(35))
        assertEquals(Species.RABBIT, Species.forRoll(65))
        assertEquals(Species.OWL, Species.forRoll(90))
        assertEquals(Species.DRAGON, Species.forRoll(98))
        assertEquals(Species.DRAGON, Species.forRoll(99))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `roll outside range is rejected`() {
        Species.forRoll(100)
    }

    @Test
    fun `draw follows the weights`() {
        val random = Random(42)
        val counts = Species.entries.associateWith { 0 }.toMutableMap()
        repeat(100_000) {
            val s = Species.draw(random)
            counts[s] = counts.getValue(s) + 1
        }
        for (s in Species.entries) {
            val share = counts.getValue(s) / 100_000.0 * 100
            assertTrue("${s.name} share $share", kotlin.math.abs(share - s.weight) < 1.0)
        }
    }

    @Test
    fun `keys round-trip`() {
        Species.entries.forEach { assertEquals(it, Species.fromKey(it.key)) }
        assertNull(Species.fromKey("unicorn"))
    }
}
