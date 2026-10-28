package com.carpet.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GrowthStageTest {

    @Test
    fun `stage boundaries`() {
        assertEquals(GrowthStage.BABY, GrowthStage.of(null))
        assertEquals(GrowthStage.BABY, GrowthStage.of(4_999f))
        assertEquals(GrowthStage.CHILD, GrowthStage.of(5_000f))
        assertEquals(GrowthStage.YOUTH, GrowthStage.of(30_000f))
        assertEquals(GrowthStage.MIDDLE, GrowthStage.of(80_000f))
        assertEquals(GrowthStage.ELDER, GrowthStage.of(150_000f))
        assertEquals(GrowthStage.ELDER, GrowthStage.of(1_000_000f))
    }

    @Test
    fun `negative odometer is treated as zero`() {
        assertEquals(GrowthStage.BABY, GrowthStage.of(-10f))
    }

    @Test
    fun `progress toward next stage`() {
        assertEquals(0.5f, GrowthStage.YOUTH.progress(55_000f), 0.0001f)
        assertEquals(1f, GrowthStage.ELDER.progress(200_000f), 0.0001f)
    }
}
