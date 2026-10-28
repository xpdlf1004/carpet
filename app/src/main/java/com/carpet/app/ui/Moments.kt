package com.carpet.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.carpet.app.domain.Celebration
import com.carpet.app.domain.Emotion
import com.carpet.app.domain.Growth
import com.carpet.app.domain.GrowthStage
import com.carpet.app.domain.Lines
import com.carpet.app.domain.Species
import com.carpet.app.domain.withSubject
import com.carpet.app.domain.withTopic
import com.carpet.app.ui.theme.Palette
import com.carpet.app.ui.theme.Space
import com.carpet.app.ui.theme.Type

/**
 * A birthday (every 2,000 km) or a new look (stage). One screen, one line from the friend,
 * one button back to the road.
 */
@Composable
fun CelebrationScreen(moment: Celebration, species: Species, name: String, onDone: () -> Unit) {
    val g = moment.growth
    val (tag, title, line) = when (moment) {
        is Celebration.NewStage -> Triple(
            "새 모습",
            "${name.withSubject()}\n${g.stage.label.withSubject()} 됐어요",
            stageLine(g.stage),
        )
        is Celebration.Birthday -> Triple(
            "생일",
            if (g.ageYears == 1) "오늘은\n${name}의 첫 돌이에요" else "${name.withSubject()}\n${g.ageYears}살이 됐어요",
            "같이 달려 줘서 고마워. 앞으로도 잘 부탁해!",
        )
    }
    PopStage(species, g, Scene.DAY, line) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.block)) {
            AccentTag(tag)
            Column(verticalArrangement = Arrangement.spacedBy(Space.tight)) {
                Text(title, style = Type.title)
                Text("누적 ${formatKm(g.odometerKm)} · 키 ${Lines.heightText(g.heightCm)} cm", style = Type.lead)
            }
            PrimaryButton(text = "계속 달려요", onClick = onDone)
        }
    }
}

private fun stageLine(stage: GrowthStage): String = when (stage) {
    GrowthStage.BABY -> "안녕! 잘 부탁해."
    GrowthStage.CHILD -> "나 이제 어린이야! 더 멀리 가 보자."
    GrowthStage.YOUTH -> "힘이 넘쳐! 어디든 데려다줄게."
    GrowthStage.MIDDLE -> "우리 오래 같이 달렸다. 고마워."
    GrowthStage.ELDER -> "천천히, 오래오래 같이 달리자."
}

/**
 * A new body (new car): its odometer starts near 0, so a new egg is coming. The friend says
 * goodbye and moves to the friend box with the km it reached.
 */
@Composable
fun FarewellScreen(species: Species, name: String, growth: Growth, onMove: () -> Unit, onStay: () -> Unit) {
    PopStage(species, growth, Scene.NURSERY, "동생 잘 부탁해. 나는 카펫함에서 지켜볼게!") {
        Column(verticalArrangement = Arrangement.spacedBy(Space.block)) {
            AccentTag("새 몸")
            Column(verticalArrangement = Arrangement.spacedBy(Space.tight)) {
                Text("새 몸에는\n새 카펫이 태어나요", style = Type.title)
                Text(
                    "${name.withTopic()} ${formatKm(growth.odometerKm)}를 함께 달린 모습 그대로\n카펫함에서 기다려요.",
                    style = Type.lead,
                )
            }
            PrimaryButton(text = "새 알 만나기", onClick = onMove)
            GhostPill("조금 더 있을게요", onClick = onStay)
        }
    }
}

/** Stage with the friend popping in happily, and [info] on the right. */
@Composable
private fun PopStage(species: Species, growth: Growth, scene: Scene, line: String, info: @Composable () -> Unit) {
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    TwoPane(
        stage = {
            StageCard(scene, Modifier.fillMaxSize()) { ground, size ->
                OnGround(ground, size) { m ->
                    Box(m) {
                        Sparkles(keepTwinkling = true, modifier = Modifier.fillMaxSize())
                        CarPetCharacter(
                            species,
                            Emotion.GLAD,
                            growth.stage,
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    transformOrigin = TransformOrigin(0.5f, 1f)
                                    scaleX = pop.value
                                    scaleY = pop.value
                                },
                            sizeRatio = growth.sizeRatio,
                        )
                    }
                }
                HeadBubble(line, ground, size, growth.sizeRatio)
            }
        },
        info = info,
    )
}

@Composable
internal fun AccentTag(text: String) {
    Surface(shape = CircleShape, color = Palette.Fox.copy(alpha = 0.14f)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = Type.chip.copy(color = Palette.Heading), maxLines = 1)
        }
    }
}
