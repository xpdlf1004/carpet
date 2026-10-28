package com.carpet.app.domain

enum class Emotion(val label: String) {
    CALM("기본"),
    HUNGRY("배고픔"),
    SICK("아픔"),
    GLAD("반가움"),
    RAINY("비 맞음"),
}

enum class ActionKind { CHARGER, REPAIR_SHOP }

data class Suggestion(val kind: ActionKind, val label: String)

/**
 * What the character shows and says right now.
 * [cause] is the plain-language reason behind the emotion (e.g. "배터리 12%").
 */
data class Mood(
    val emotion: Emotion,
    val line: String,
    val cause: String? = null,
    val suggestion: Suggestion? = null,
)
