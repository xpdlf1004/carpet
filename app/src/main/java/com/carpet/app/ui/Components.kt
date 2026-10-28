package com.carpet.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carpet.app.ui.theme.Palette
import com.carpet.app.ui.theme.Type

/** Filled pill action (42dot graphite). One per screen. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg = when {
        !enabled -> Palette.Mist
        pressed -> Palette.Slate
        else -> Palette.Graphite
    }
    val fg = if (enabled) Palette.White else Palette.Faint
    Box(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.button, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Small outlined pill (secondary, e.g. the demo toggle). */
@Composable
fun GhostPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .border(1.dp, Palette.Hairline, CircleShape)
            .background(Palette.White)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.chip, color = Palette.Body, maxLines = 1)
    }
}

/** Read-only status chip on mist. */
@Composable
fun InfoChip(text: String, alert: Boolean = false) {
    Surface(shape = CircleShape, color = if (alert) Palette.Fox.copy(alpha = 0.16f) else Palette.Mist) {
        Text(
            text,
            style = if (alert) Type.chip.copy(color = Palette.Heading) else Type.chip,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

/** Tappable choice chip; selected = graphite. */
@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Palette.Graphite else Palette.White
    val fg = if (selected) Palette.White else Palette.Body
    Surface(
        shape = CircleShape,
        color = bg,
        border = if (selected) null else BorderStroke(1.dp, Palette.Hairline),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(text, style = Type.chip, color = fg, maxLines = 1)
        }
    }
}

/** Thin progress bar in the fox's orange. */
@Composable
fun GrowthBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(Palette.Mist),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                .height(8.dp)
                .clip(CircleShape)
                .background(Palette.Fox),
        )
    }
}

/**
 * A few numbers side by side, label above value (「오늘 달린 거리 / 23 km」).
 * Cells share the width by how long their text is and are split by hairlines.
 */
@Composable
fun StatRow(cells: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val valueStyle = Type.speech.copy(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, color = Palette.Heading)
    Surface(shape = RoundedCornerShape(20.dp), color = Palette.Mist, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            cells.forEachIndexed { i, (label, value) ->
                if (i > 0) Box(Modifier.width(1.dp).height(36.dp).background(Palette.Hairline))
                // Each cell gets room by its measured text, so 「150,000 km」 and 「100 cm」 both fit.
                val share = maxOf(
                    measurer.measure(label, Type.caption).size.width,
                    measurer.measure(value, valueStyle).size.width,
                ).toFloat() + 1f
                Column(
                    Modifier.weight(share).padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(label, style = Type.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(value, style = valueStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/**
 * Korean text otherwise wraps between any two syllables ("고/파"). A WORD JOINER between the
 * characters of each word leaves spaces as the only break points.
 */
internal fun String.keepWordsTogether(): String =
    split(' ').joinToString(" ") { word -> word.toList().joinToString("⁠") }

internal val NoColor = Color.Transparent
