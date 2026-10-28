package com.carpet.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GrowthTest {

    @Test
    fun `age counts months for babies and years after the first birthday`() {
        assertEquals("갓 태어남", Growth(0f).ageLabel)
        assertEquals("1개월", Growth(170f).ageLabel)
        assertEquals("11개월", Growth(1_999f).ageLabel)
        assertEquals("1살", Growth(2_000f).ageLabel)
        assertEquals("15살", Growth(30_000f).ageLabel)
        assertEquals("75살", Growth(150_000f).ageLabel)
    }

    @Test
    fun `stages land on human ages`() {
        assertEquals(GrowthStage.CHILD, Growth(5_000f).stage)
        assertEquals(2, Growth(5_000f).ageYears)
        assertEquals(15, Growth(GrowthStage.YOUTH.fromKm.toFloat()).ageYears)
        assertEquals(40, Growth(GrowthStage.MIDDLE.fromKm.toFloat()).ageYears)
        assertEquals(75, Growth(GrowthStage.ELDER.fromKm.toFloat()).ageYears)
    }

    @Test
    fun `height grows until youth then stays`() {
        assertEquals(30f, Growth(0f).heightCm, 0.001f)
        assertEquals(50f, Growth(5_000f).heightCm, 0.001f)
        assertEquals(100f, Growth(30_000f).heightCm, 0.001f)
        assertEquals(100f, Growth(90_000f).heightCm, 0.001f)
        assertTrue(Growth(31f).heightCm > Growth(0f).heightCm)
    }

    @Test
    fun `drawn size follows height`() {
        assertEquals(Growth.MIN_SIZE, Growth(0f).sizeRatio, 0.001f)
        assertEquals(1f, Growth(30_000f).sizeRatio, 0.001f)
        assertTrue(Growth(4_000f).sizeRatio < Growth(6_000f).sizeRatio)
    }

    @Test
    fun `next goal is the birthday, or the stage when it comes first`() {
        val baby = Growth(500f).nextGoal
        assertEquals("1살", baby.label)
        assertEquals(1_500f, baby.kmLeft, 0.001f)
        assertEquals(0.25f, baby.progress, 0.001f)

        val almostChild = Growth(4_500f).nextGoal
        assertEquals("어린이", almostChild.label)
        assertEquals(500f, almostChild.kmLeft, 0.001f)
        assertEquals(0.5f, almostChild.progress, 0.001f)

        val newChild = Growth(5_000f).nextGoal
        assertEquals("3살", newChild.label)
        assertEquals(0f, newChild.progress, 0.001f)

        assertEquals("76살", Growth(150_000f).nextGoal.label)
    }

    @Test
    fun `celebrations`() {
        assertNull(Celebration.between(Growth(100f), Growth(200f)))
        assertTrue(Celebration.between(Growth(1_990f), Growth(2_010f)) is Celebration.Birthday)
        assertTrue(Celebration.between(Growth(4_990f), Growth(5_010f)) is Celebration.NewStage)
        // A stage change on a birthday is celebrated as the stage.
        assertTrue(Celebration.between(Growth(29_990f), Growth(30_010f)) is Celebration.NewStage)
        assertNull(Celebration.between(Growth(5_000f), Growth(100f)))
    }

    @Test
    fun `particles`() {
        assertEquals("3살이", "3살".withSubject())
        assertEquals("청년이", "청년".withSubject())
        assertEquals("어린이가", "어린이".withSubject())
        assertEquals("콩이와", "콩이".withAnd())
        assertEquals("달빛은", "달빛".withTopic())
    }
}
