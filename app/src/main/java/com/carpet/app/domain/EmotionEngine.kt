package com.carpet.app.domain

import com.carpet.app.vehicle.FluidLevel
import com.carpet.app.vehicle.VehicleSignals

/**
 * Turns vehicle signals into one emotion. The character IS the car, so it speaks about the
 * car's body in the first person ("앞유리가 다 젖었어", "내가 태워 줄게").
 *
 * Priority: urgent sickness > hunger > mild sickness > greeting > rain > calm.
 * Urgent sickness (brake fluid, engine oil critically low, power-down warning) beats hunger
 * because it is a safety issue; a low battery beats a low washer fluid.
 *
 * The words come from [Lines]: several lines per situation, chosen with the time and today's
 * drive in mind, so the friend does not repeat itself.
 */
object EmotionEngine {

    const val HUNGRY_PERCENT = 20
    const val STARVING_PERCENT = 10
    const val GREETING_MILLIS = 15_000L

    private val REPAIR = Suggestion(ActionKind.REPAIR_SHOP, "가까운 정비소 들르기")
    private val CHARGER = Suggestion(ActionKind.CHARGER, "가까운 충전소 들르기")

    fun decide(
        signals: VehicleSignals,
        boardedAtMillis: Long?,
        nowMillis: Long,
        context: LineContext = LineContext(),
    ): Mood {
        val slot = nowMillis / Lines.ROTATE_MILLIS
        fun say(lines: List<String>, salt: String) = Lines.pick(lines, slot, salt)

        urgentSickness(signals, ::say)?.let { return it }

        val battery = signals.batteryPercent
        if (battery != null && battery <= HUNGRY_PERCENT) {
            val line = if (battery <= STARVING_PERCENT) say(Lines.starving, "starving") else say(Lines.hungry, "hungry")
            return Mood(Emotion.HUNGRY, line, "배터리 $battery% 남음", CHARGER)
        }

        mildSickness(signals, ::say)?.let { return it }

        if (boardedAtMillis != null && nowMillis - boardedAtMillis in 0..GREETING_MILLIS) {
            // One line per boarding: seeded by the boarding time, not the clock.
            val line = Lines.pick(Lines.greeting(signals.companionCount, context), boardedAtMillis, "greeting")
            return Mood(Emotion.GLAD, line)
        }

        if (signals.wiperOn == true) {
            return Mood(Emotion.RAINY, say(Lines.rainy, "rainy"), "와이퍼 작동 중")
        }

        // Every other slot, a close birthday or stage takes the floor.
        val near = context.growth?.let { Lines.goalNear(it.nextGoal) }
        if (near != null && slot % 2 == 0L) return Mood(Emotion.CALM, near)
        return Mood(Emotion.CALM, say(Lines.calm(battery, context), "calm"))
    }

    private fun urgentSickness(s: VehicleSignals, say: (List<String>, String) -> String): Mood? = when {
        s.brakeFluid == FluidLevel.LOW || s.brakeFluid == FluidLevel.CRITICALLY_LOW ->
            Mood(Emotion.SICK, say(Lines.brakeFluid, "brake"), "브레이크액 부족", REPAIR)
        s.engineOil == FluidLevel.CRITICALLY_LOW ->
            Mood(Emotion.SICK, say(Lines.oilCritical, "oil!"), "엔진오일 매우 부족", REPAIR)
        s.powerDownWarning == true ->
            Mood(Emotion.SICK, say(Lines.powerDown, "power"), "출력 저하 경고", REPAIR)
        else -> null
    }

    private fun mildSickness(s: VehicleSignals, say: (List<String>, String) -> String): Mood? = when {
        s.engineOil == FluidLevel.LOW ->
            Mood(Emotion.SICK, say(Lines.oilLow, "oil"), "엔진오일 부족", REPAIR)
        s.washerFluidLow == true ->
            Mood(Emotion.SICK, say(Lines.washer, "washer"), "워셔액 부족", REPAIR)
        else -> null
    }
}
