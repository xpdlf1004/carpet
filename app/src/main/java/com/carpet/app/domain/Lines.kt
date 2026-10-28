package com.carpet.app.domain

import kotlin.math.roundToInt

/**
 * What the friend knows besides the vehicle signals, so it can say something that fits today:
 * the time, how far we went today, how grown it is, how long it waited.
 */
data class LineContext(
    /** 0..23 */
    val hour: Int = 12,
    /** 1 = Monday .. 7 = Sunday */
    val dayOfWeek: Int = 3,
    /** 1..12 */
    val month: Int = 5,
    val todayKm: Float? = null,
    val growth: Growth? = null,
    /** Days since the last day we drove together, counted when the driver sits down. */
    val daysAway: Int? = null,
)

/**
 * Line pools. The friend IS the car and talks casually (반말) about its own body. Never say 「차」:
 * the friend does not call itself a car.
 *
 * A line is picked from the pool with a seed, so it stays put for a while instead of changing
 * every second: the greeting keeps one line per boarding, the rest rotate every few minutes.
 */
internal object Lines {

    const val ROTATE_MILLIS = 4 * 60_000L

    fun pick(lines: List<String>, seed: Long, salt: String): String {
        require(lines.isNotEmpty())
        val mixed = seed * 31 + salt.hashCode()
        return lines[Math.floorMod(mixed, lines.size.toLong()).toInt()]
    }

    // ---- Hunger, sickness, rain ----

    val starving = listOf(
        "배가 너무 고파. 충전소부터 가자.",
        "더는 못 버티겠어. 충전소 들러 줘.",
        "힘이 다 빠져 가. 밥 먹으러 가자.",
    )
    val hungry = listOf(
        "배고파. 충전하고 싶어.",
        "슬슬 배고프다. 충전소 들를까?",
        "배에서 꼬르륵 소리 나. 충전하자.",
    )
    val brakeFluid = listOf(
        "브레이크가 약해졌어. 브레이크액이 부족해.",
        "멈추는 게 힘들어. 브레이크액을 채워 줘.",
    )
    val oilCritical = listOf(
        "속이 많이 아파. 엔진오일이 거의 없어.",
        "속이 너무 아파. 엔진오일부터 봐 줘.",
    )
    val powerDown = listOf(
        "힘이 안 나. 점검이 필요해.",
        "몸이 무거워. 점검받고 싶어.",
    )
    val oilLow = listOf(
        "속이 안 좋아. 엔진오일이 부족해.",
        "배 속이 꾸룩거려. 엔진오일이 모자라.",
    )
    val washer = listOf(
        "앞이 뿌예. 워셔액이 떨어졌어.",
        "눈이 따가워. 워셔액 좀 채워 줘.",
    )
    val rainy = listOf(
        "비 온다. 앞유리가 다 젖었어.",
        "빗소리 들려? 천천히 가자.",
        "비 오는 날엔 내가 더 조심할게.",
        "와이퍼가 열심히 일하는 중이야.",
    )

    // ---- Greeting (the driver just sat down) ----

    fun greeting(companions: Int, c: LineContext): List<String> {
        val away = c.daysAway
        if (away != null && away >= 3) {
            return listOf(
                "오랜만이야. ${away}일 동안 보고 싶었어.",
                "왔구나! 많이 기다렸어.",
            )
        }
        if (companions >= 2) {
            return listOf(
                "오늘은 북적북적하다. 다들 안전벨트 했지?",
                "와, 다 같이 탔네. 천천히 갈게.",
            )
        }
        if (companions == 1) {
            return listOf(
                "오늘은 둘이네. 반가워.",
                "옆자리에 누가 탔다. 안녕!",
                "오늘은 다 같이 탔네. 천천히 갈게.",
            )
        }
        val today = c.todayKm
        if (today != null && today >= 1f) {
            return listOf(
                "또 왔네. 아까 거기 잘 다녀왔어?",
                "다시 만나서 반가워.",
            )
        }
        return byTime(c.hour) + byDay(c.dayOfWeek)
    }

    private fun byTime(hour: Int): List<String> = when (hour) {
        in 0..4 -> listOf("이 시간에 나왔구나. 조심히 가자.", "졸리면 꼭 쉬었다 가자.")
        in 5..10 -> listOf("좋은 아침. 오늘도 내가 태워 줄게.", "아침 공기 좋다. 출발해 볼까?")
        in 11..13 -> listOf("점심은 먹었어? 나는 든든해.", "점심시간이네. 어디 가?")
        in 14..17 -> listOf("오후도 힘내자. 같이 가 줄게.", "왔구나. 오늘도 내가 태워 줄게.")
        in 18..21 -> listOf("오늘 하루 고생 많았어. 집에 가자.", "저녁이다. 천천히 가자.")
        else -> listOf("밤길이야. 내가 앞을 환하게 비춰 줄게.", "늦었네. 조심히 데려다줄게.")
    }

    private fun byDay(day: Int): List<String> = when (day) {
        1 -> listOf("월요일이다. 같이 힘내 보자.")
        5 -> listOf("금요일이다! 조금만 더 힘내.")
        6, 7 -> listOf("주말이다. 멀리 가 볼까?")
        else -> emptyList()
    }

    // ---- Calm (nothing urgent) ----

    fun calm(batteryPercent: Int?, c: LineContext): List<String> = buildList {
        add("오늘도 잘 달려 볼게.")
        add("같이 달리니까 좋다.")
        add("어디로 갈지 정해지면 알려 줘.")

        val today = c.todayKm
        when {
            today == null -> Unit
            today < 1f -> add("오늘은 아직 안 달렸어. 어디 갈까?")
            today < 100f -> add("오늘 벌써 ${today.roundToInt()} km 같이 달렸어.")
            today < 300f -> add("오늘 ${today.roundToInt()} km나 달렸다. 쉬었다 가자.")
            else -> add("오늘 정말 멀리 왔다. 졸리면 꼭 쉬자.")
        }

        c.growth?.let { g ->
            when (g.stage) {
                GrowthStage.BABY -> {
                    add("나 오늘도 컸어? 키가 ${heightText(g.heightCm)} cm래.")
                    add("빨리 커서 멀리 가 보고 싶어.")
                }
                GrowthStage.CHILD -> {
                    add("나 이제 ${g.ageYears}살이야. 많이 컸지?")
                    add("키가 ${heightText(g.heightCm)} cm야. 더 클 거야.")
                }
                GrowthStage.YOUTH -> add("요즘 힘이 넘쳐. 어디든 가자.")
                GrowthStage.MIDDLE -> add("우리 꽤 오래 같이 달렸다.")
                GrowthStage.ELDER -> add("이 길, 예전에도 같이 달렸지?")
            }
        }

        if (batteryPercent != null && batteryPercent >= 80) add("배가 든든해. 멀리 가도 돼.")

        when (c.hour) {
            in 22..23, in 0..4 -> add("밤이 깊었어. 천천히 가자.")
            in 5..9 -> add("아침 햇살 좋다.")
            else -> Unit
        }

        add(
            when (c.month) {
                3, 4, 5 -> "봄이다. 꽃 보러 갈까?"
                6, 7, 8 -> "여름이다. 바다 보러 갈까?"
                9, 10, 11 -> "단풍 보러 가고 싶다."
                else -> "추워도 같이 달리면 따뜻해."
            },
        )
    }

    /** Shown now and then when the next birthday or stage is close. */
    fun goalNear(goal: Goal): String? =
        if (goal.kmLeft in 0.5f..150f) "${goal.kmLeft.roundToInt()} km만 더 가면 나 ${goal.label.withSubject()} 돼!" else null

    fun heightText(cm: Float): String {
        val tenths = (cm * 10).roundToInt()
        return if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
    }
}
