package com.carpet.app.domain

import kotlin.math.floor

/**
 * How grown the friend is. The friend IS the car, so it ages with the car's own odometer:
 * a new car hatches a baby, a car with many km is an older friend, and a new car (odometer back
 * near 0) means a new egg while the old friend rests in the friend box.
 *
 * 2,000 km = 1 year of age, so the five stages land on human ages
 * (어린이 2살 반, 청년 15살, 중년 40살, 노년 75살). A private car runs about 900 km a month
 * (한국교통안전공단 2025, 비사업용 하루 30.8 km), so a baby turns a month older every few days
 * and every friend has a birthday about every two months. Height grows until 청년.
 */
data class Growth(val odometerKm: Float) {

    private val km = odometerKm.coerceAtLeast(0f)

    val stage: GrowthStage = GrowthStage.of(km)
    val ageMonths: Int = floor(km / KM_PER_MONTH).toInt()
    val ageYears: Int = ageMonths / 12
    val heightCm: Float = heightAt(km)

    /** Drawn height relative to the grown-up drawing. */
    val sizeRatio: Float = MIN_SIZE + (1f - MIN_SIZE) * (heightCm - BIRTH_CM) / (ADULT_CM - BIRTH_CM)

    /** 「갓 태어남」, 「4개월」, 「7살」 */
    val ageLabel: String = when {
        ageMonths < 1 -> "갓 태어남"
        ageMonths < 12 -> "${ageMonths}개월"
        else -> "${ageYears}살"
    }

    /** The next thing to look forward to: the next birthday, or a new stage when that comes first. */
    val nextGoal: Goal = run {
        val birthdayKm = (ageYears + 1) * KM_PER_YEAR
        val lastBirthdayKm = ageYears * KM_PER_YEAR
        val next = stage.next
        if (next != null && next.fromKm <= birthdayKm) {
            val start = maxOf(lastBirthdayKm, stage.fromKm.toFloat())
            Goal(next.label, next.fromKm - km, fraction(km, start, next.fromKm.toFloat()))
        } else {
            val start = maxOf(lastBirthdayKm, stage.fromKm.toFloat())
            Goal("${ageYears + 1}살", birthdayKm - km, fraction(km, start, birthdayKm))
        }
    }

    companion object {
        const val KM_PER_YEAR = 2_000f
        const val KM_PER_MONTH = KM_PER_YEAR / 12f
        const val BIRTH_CM = 30f
        const val CHILD_CM = 50f
        const val ADULT_CM = 100f
        const val MIN_SIZE = 0.58f

        fun heightAt(km: Float): Float {
            val child = GrowthStage.CHILD.fromKm.toFloat()
            val youth = GrowthStage.YOUTH.fromKm.toFloat()
            return when {
                km <= child -> BIRTH_CM + (CHILD_CM - BIRTH_CM) * km.coerceAtLeast(0f) / child
                km <= youth -> CHILD_CM + (ADULT_CM - CHILD_CM) * (km - child) / (youth - child)
                else -> ADULT_CM
            }
        }

        private fun fraction(km: Float, start: Float, end: Float): Float =
            if (end <= start) 1f else ((km - start) / (end - start)).coerceIn(0f, 1f)
    }
}

/** [label] is what the friend becomes: 「3살」 or 「청년」. */
data class Goal(val label: String, val kmLeft: Float, val progress: Float)

/** A moment worth stopping for: a new stage beats a birthday when both happen at once. */
sealed interface Celebration {
    val growth: Growth

    data class NewStage(override val growth: Growth) : Celebration
    data class Birthday(override val growth: Growth) : Celebration

    companion object {
        fun between(before: Growth, after: Growth): Celebration? = when {
            after.stage.ordinal > before.stage.ordinal -> NewStage(after)
            after.ageYears > before.ageYears -> Birthday(after)
            else -> null
        }
    }
}

/** Age and stage already celebrated. */
data class Milestone(val age: Int, val stage: Int) : Comparable<Milestone> {
    override fun compareTo(other: Milestone): Int = compareValuesBy(this, other, { it.stage }, { it.age })

    companion object {
        fun of(g: Growth) = Milestone(g.ageYears, g.stage.ordinal)
    }
}

/**
 * Decides when to show a birthday or a new stage. Real growth is compared with what was
 * already celebrated (saved); demo values are compared with what is on screen, so a demo preset
 * shows the moment without using up, or blocking, the real ones.
 */
class CelebrationTracker {
    private var seen: Milestone? = null
    private var wasDemo = false

    fun forget() {
        seen = null
    }

    /** Right after hatching: what is on screen now needs no celebration. */
    fun startAt(shown: Growth, demo: Boolean) {
        seen = Milestone.of(shown)
        wasDemo = demo
    }

    fun observe(g: Growth, demo: Boolean, saved: Milestone): Celebration? {
        var s = seen ?: saved
        if (demo != wasDemo) {
            // Back to real values: compare with the real record again.
            if (!demo) s = saved
            wasDemo = demo
        }
        val now = Milestone.of(g)
        val moment = when {
            now.stage > s.stage -> Celebration.NewStage(g)
            now.age > s.age -> Celebration.Birthday(g)
            else -> null
        }
        // Demo values may step back down (presets), real ones only go up.
        seen = if (demo) now else maxOf(s, now)
        return moment
    }
}
