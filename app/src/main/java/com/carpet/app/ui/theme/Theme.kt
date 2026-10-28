package com.carpet.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Near-monochrome palette after 42dot (the maker of Pleos Connect): the character is the only
 * colour on screen. Text uses the system sans-serif, which Pleos maps to 42dot's Asta Sans.
 */
object Palette {
    val Heading = Color(0xFF0D0D0D)
    val Ink = Color(0xFF171717)
    val Body = Color(0xFF333333)
    val Muted = Color(0xFF666666)
    val Slate = Color(0xFF737D8C)
    val Faint = Color(0xFFAAAAAA)
    val Graphite = Color(0xFF32353F)
    val Mist = Color(0xFFF6F6F9)
    val Hairline = Color(0xFFDDDDDD)
    val White = Color(0xFFFFFFFF)

    /** The fox's orange, sampled from the character art. Used sparingly (growth bar). */
    val Fox = Color(0xFFF09848)
    val Rain = Color(0xFF7DB9F2)
}

/** One set of margins for every screen. */
object Space {
    val screenH = 48.dp
    val screenV = 40.dp
    val columnGap = 48.dp
    val block = 20.dp
    val tight = 8.dp
}

/** Type scale. Line heights are explicit so wrapped Korean never overlaps or clips. */
object Type {
    val name = TextStyle(fontSize = 40.sp, lineHeight = 52.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em, color = Palette.Heading)
    val title = TextStyle(fontSize = 34.sp, lineHeight = 46.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em, color = Palette.Heading)
    val speech = TextStyle(fontSize = 28.sp, lineHeight = 40.sp, fontWeight = FontWeight.Medium, color = Palette.Ink)
    val lead = TextStyle(fontSize = 19.sp, lineHeight = 28.sp, fontWeight = FontWeight.Normal, color = Palette.Muted)
    val meta = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium, color = Palette.Muted)
    val button = TextStyle(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold)
    val chip = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium, color = Palette.Body)
    val caption = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, color = Palette.Slate)
    val label = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, color = Palette.Slate)
    val body = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal, color = Palette.Body)
}

private val colors = lightColorScheme(
    primary = Palette.Graphite,
    onPrimary = Palette.White,
    secondary = Palette.Fox,
    onSecondary = Palette.White,
    background = Palette.White,
    onBackground = Palette.Ink,
    surface = Palette.White,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.Mist,
    onSurfaceVariant = Palette.Body,
    outline = Palette.Hairline,
)

// Material's fixed line heights make large Korean text overlap when it wraps.
private val typography = Typography().run {
    copy(
        bodyLarge = bodyLarge.copy(lineHeight = TextUnit.Unspecified),
        bodyMedium = bodyMedium.copy(lineHeight = TextUnit.Unspecified),
        labelLarge = labelLarge.copy(lineHeight = TextUnit.Unspecified),
    )
}

@Composable
fun CarPetTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
