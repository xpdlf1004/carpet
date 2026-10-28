package com.carpet.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carpet.app.ui.theme.Space

/**
 * Shared screen frame. Wide: stage card on the left (fills the height), info on the right,
 * vertically centred, max 560dp wide. Narrow: stage on top, info below, scrolling.
 * Margins are the same on every screen (Space.screenH / screenV).
 */
@Composable
fun TwoPane(stage: @Composable () -> Unit, info: @Composable () -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(horizontal = Space.screenH, vertical = Space.screenV),
    ) {
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Space.columnGap)) {
                Box(Modifier.weight(1f).fillMaxHeight()) { stage() }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                    Box(Modifier.widthIn(max = 560.dp).verticalScroll(rememberScrollState())) { info() }
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Box(Modifier.fillMaxWidth().aspectRatio(0.9f)) { stage() }
                Spacer(Modifier.height(Space.block))
                info()
            }
        }
    }
}
