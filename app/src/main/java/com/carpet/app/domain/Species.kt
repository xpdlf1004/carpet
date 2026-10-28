package com.carpet.app.domain

import kotlin.random.Random

enum class Rarity(val label: String) { COMMON("일반"), RARE("희귀"), LEGENDARY("전설") }

/**
 * Characters that can hatch from the egg. Each wears a car part.
 * [weight] is the draw weight; the odds are shown to the user as-is.
 */
enum class Species(
    val key: String,
    val label: String,
    val carPart: String,
    val rarity: Rarity,
    val weight: Int,
) {
    FOX("fox", "플러그 여우", "플러그 꼬리", Rarity.COMMON, 35),
    TURTLE("turtle", "타이어 거북", "타이어 등껍질", Rarity.COMMON, 30),
    RABBIT("rabbit", "깜빡이 토끼", "방향지시등 귀", Rarity.COMMON, 25),
    OWL("owl", "헤드라이트 부엉이", "전조등 눈", Rarity.RARE, 8),
    DRAGON("dragon", "터보 드래곤", "배기구 꼬리", Rarity.LEGENDARY, 2);

    /** Chance in percent, from the weights. */
    val chancePercent: Int get() = weight * 100 / TOTAL_WEIGHT

    companion object {
        val TOTAL_WEIGHT: Int = entries.sumOf { it.weight }

        fun fromKey(key: String?): Species? = entries.firstOrNull { it.key == key }

        /** [roll] in 0 until TOTAL_WEIGHT. */
        fun forRoll(roll: Int): Species {
            require(roll in 0 until TOTAL_WEIGHT) { "roll out of range: $roll" }
            var acc = 0
            for (s in entries) {
                acc += s.weight
                if (roll < acc) return s
            }
            error("unreachable")
        }

        fun draw(random: Random = Random.Default): Species = forRoll(random.nextInt(TOTAL_WEIGHT))
    }
}
