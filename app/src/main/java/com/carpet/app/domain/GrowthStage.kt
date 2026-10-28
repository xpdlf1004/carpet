package com.carpet.app.domain

/** The character ages with the car's odometer. */
enum class GrowthStage(val label: String, val fromKm: Int) {
    BABY("아기", 0),
    CHILD("어린이", 5_000),
    YOUTH("청년", 30_000),
    MIDDLE("중년", 80_000),
    ELDER("노년", 150_000);

    val next: GrowthStage? get() = entries.getOrNull(ordinal + 1)

    /** 0..1 progress toward the next stage; 1 for the last stage. */
    fun progress(odometerKm: Float): Float {
        val nextKm = next?.fromKm ?: return 1f
        return ((odometerKm - fromKm) / (nextKm - fromKm)).coerceIn(0f, 1f)
    }

    companion object {
        fun of(odometerKm: Float?): GrowthStage {
            val km = (odometerKm ?: 0f).coerceAtLeast(0f)
            return entries.last { km >= it.fromKm }
        }
    }
}
