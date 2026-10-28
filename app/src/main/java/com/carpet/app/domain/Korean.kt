package com.carpet.app.domain

private fun Char.hasFinalConsonant(): Boolean = this in '가'..'힣' && (this - '가') % 28 != 0

/** "여우" -> "여우가", "거북" -> "거북이", "3살" -> "3살이": picks the subject particle by the final consonant. */
fun String.withSubject(): String {
    val c = lastOrNull() ?: return this
    return this + if (c.hasFinalConsonant()) "이" else "가"
}

/** "콩이" -> "콩이예요", "달빛" -> "달빛이에요". */
fun String.withCopula(): String {
    val c = lastOrNull() ?: return this
    return this + if (c.hasFinalConsonant()) "이에요" else "예요"
}

/** "콩이" -> "콩이와", "달빛" -> "달빛과". */
fun String.withAnd(): String {
    val c = lastOrNull() ?: return this
    return this + if (c.hasFinalConsonant()) "과" else "와"
}

/** "콩이" -> "콩이는", "달빛" -> "달빛은". */
fun String.withTopic(): String {
    val c = lastOrNull() ?: return this
    return this + if (c.hasFinalConsonant()) "은" else "는"
}
