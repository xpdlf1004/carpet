package com.carpet.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.carpet.app.ui.theme.Palette
import com.carpet.app.ui.theme.Type

/** Lucide icons (24x24, 2px round stroke), drawn in the current tint. */
object CarPetIcons {
    /** Lucide 「archive」: the box where the carpets rest. */
    val Archive: ImageVector = lucide(
        "archive",
        "M3 3h18a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z",
        "M4 8v11a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8",
        "M10 12h4",
    )

    /** Lucide 「x」 */
    val Close: ImageVector = lucide("x", "M18 6 6 18", "m6 6 12 12")

    private fun lucide(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                paths.forEach { d ->
                    addPath(
                        pathData = PathParser().parsePathString(d).toNodes(),
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }
            .build()
}

/** White pill with an icon and a label (「카펫함」, 「닫기」). */
@Composable
fun IconPill(icon: ImageVector, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = Palette.White, border = BorderStroke(1.dp, Palette.Hairline), modifier = modifier) {
        Row(
            Modifier
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClick)
                .heightIn(min = 52.dp)
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = Palette.Ink, modifier = Modifier.size(22.dp))
            Text(text, style = Type.chip.copy(color = Palette.Ink))
        }
    }
}
