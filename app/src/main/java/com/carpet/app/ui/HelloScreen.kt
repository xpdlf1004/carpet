package com.carpet.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.carpet.app.domain.Emotion
import com.carpet.app.domain.Growth
import com.carpet.app.domain.GrowthStage
import com.carpet.app.domain.Rarity
import com.carpet.app.domain.Species
import com.carpet.app.domain.withCopula
import com.carpet.app.ui.theme.Palette
import com.carpet.app.ui.theme.Space
import com.carpet.app.ui.theme.Type
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val MILLIS_PER_STAGE = 1_100

/**
 * Right after hatching: a flash, the friend pops out with sparkles, and introduces itself.
 *
 * The friend ages with the odometer, so in a car that already ran far it would hatch straight
 * into an older stage. Instead of skipping, it first plays 「지나온 길」: the km count rolls up
 * from 0 while the friend grows through each stage it missed. A new car (0 km) has nothing to
 * replay and starts as a baby. Tapping the stage skips the replay.
 */
@Composable
fun HelloScreen(species: Species, name: String, growth: Growth, onStart: () -> Unit) {
    val targetKm = growth.odometerKm.coerceAtLeast(0f)
    val replay = growth.stage != GrowthStage.BABY
    val shownKm = remember { Animatable(if (replay) 0f else targetKm) }
    var done by remember { mutableStateOf(!replay) }
    val pop = remember { Animatable(if (replay) 1f else 0.4f) }
    val flash = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        launch { flash.animateTo(0f, tween(700)) }
        if (replay) {
            // Equal time per stage: the km spans differ a lot (5,000 vs 70,000), but each look should be seen.
            try {
                for ((from, to) in stageSpans(targetKm)) {
                    if (done) break
                    shownKm.snapTo(from)
                    shownKm.animateTo(to, tween(MILLIS_PER_STAGE, easing = LinearEasing))
                }
            } catch (e: CancellationException) {
                // A tap skipped the replay (it interrupts the animation); leave only if the screen went away.
                if (!isActive) throw e
            }
            done = true
            pop.snapTo(0.88f)
        }
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    val now = Growth(shownKm.value)
    TwoPane(
        stage = {
            StageCard(Scene.NURSERY, Modifier.fillMaxSize()) { ground, size ->
                OnGround(ground, size) { m ->
                    Box(
                        m.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (!done) scope.launch {
                                done = true
                                shownKm.stop()
                                shownKm.snapTo(targetKm)
                            }
                        },
                    ) {
                        // A burst for every new look
                        key(now.stage) {
                            Sparkles(keepTwinkling = done && species.rarity != Rarity.COMMON, modifier = Modifier.fillMaxSize())
                        }
                        CarPetCharacter(
                            species,
                            if (done) Emotion.GLAD else Emotion.CALM,
                            now.stage,
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    transformOrigin = TransformOrigin(0.5f, 1f)
                                    scaleX = pop.value
                                    scaleY = pop.value
                                },
                            sizeRatio = now.sizeRatio,
                        )
                    }
                }
                // Light from the cracked egg fading out
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = flash.value }
                        .background(Palette.White),
                )
            }
        },
        info = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.block)) {
                if (done) RarityTag(species) else StageTag(now)
                Column(verticalArrangement = Arrangement.spacedBy(Space.tight)) {
                    if (done) {
                        Text("안녕하세요,\n저는 ${name.withCopula()}", style = Type.title)
                        Text(introLine(growth), style = Type.lead)
                    } else {
                        Text("이 몸은 벌써\n이만큼 달렸대요", style = Type.title)
                        Text(formatKm(shownKm.value), style = Type.name.copy(color = Palette.Fox))
                    }
                }
                AnimatedVisibility(visible = done, enter = fadeIn(tween(300))) {
                    PrimaryButton(text = "출발해요", onClick = onStart)
                }
            }
        },
    )
}

private fun introLine(g: Growth): String =
    if (g.stage == GrowthStage.BABY) {
        "오늘부터 함께 달려요.\n배고프거나 아프면 먼저 말할게요."
    } else {
        "벌써 ${formatKm(g.odometerKm)}를 달려서 ${g.ageLabel.withCopula()}\n오늘부터 함께 달려요."
    }

/** (from, to) km for each stage up to [km], so the replay can give each stage the same time. */
private fun stageSpans(km: Float): List<Pair<Float, Float>> =
    GrowthStage.entries.mapNotNull { s ->
        val from = s.fromKm.toFloat()
        val to = (s.next?.fromKm?.toFloat() ?: Float.MAX_VALUE).coerceAtMost(km)
        if (from < km) from to to else null
    }

/** 「청년 · 15살」 while the replay runs. */
@Composable
private fun StageTag(g: Growth) {
    Surface(shape = CircleShape, color = Palette.Fox.copy(alpha = 0.14f)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${g.stage.label} · ${g.ageLabel}", style = Type.chip.copy(color = Palette.Heading), maxLines = 1)
        }
    }
}

/** Rarity · species · its car part, e.g. 「희귀 · 헤드라이트 부엉이 · 전조등 눈」. */
@Composable
internal fun RarityTag(species: Species) {
    val accent = species.rarity != Rarity.COMMON
    Surface(shape = CircleShape, color = if (accent) Palette.Fox.copy(alpha = 0.14f) else Palette.Mist) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${species.rarity.label} · ${species.label} · ${species.carPart}",
                style = Type.chip.copy(color = if (accent) Palette.Heading else Palette.Body),
                maxLines = 1,
            )
        }
    }
}
