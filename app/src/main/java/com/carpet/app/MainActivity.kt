package com.carpet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.carpet.app.ui.CarPetApp
import com.carpet.app.ui.theme.CarPetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CarPetTheme {
                CarScaled {
                    CarPetApp()
                }
            }
        }
    }
}

/**
 * Car displays run at low density (Pleos: 160dpi), so the app area is very large in dp and the
 * layout, designed around ~560dp of height, looks tiny. Scale dp/sp with the window size.
 */
@androidx.compose.runtime.Composable
private fun CarScaled(content: @androidx.compose.runtime.Composable () -> Unit) {
    BoxWithConstraints {
        val shortSide = if (maxWidth < maxHeight) maxWidth else maxHeight
        val scale = (shortSide / 560.dp).coerceIn(1f, 1.8f)
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density * scale, density.fontScale)) {
            content()
        }
    }
}
