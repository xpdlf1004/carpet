package com.carpet.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carpet.app.domain.ActionKind
import com.carpet.app.domain.EmotionEngine
import com.carpet.app.domain.Lines
import com.carpet.app.domain.Species
import com.carpet.app.domain.withSubject
import com.carpet.app.ui.theme.Space
import com.carpet.app.ui.theme.Type
import com.carpet.app.vehicle.FluidLevel
import com.carpet.app.vehicle.SourceStatus
import java.text.NumberFormat
import java.util.concurrent.TimeUnit

/**
 * Home: the character on its stage (scene follows the mood) on the left,
 * one message and one action on the right.
 */
@Composable
fun HomeScreen(state: CarPetUiState, onSuggestion: (ActionKind) -> Unit) {
    val species = state.species ?: Species.FOX
    TwoPane(
        stage = {
            StageCard(sceneFor(state.mood.emotion), Modifier.fillMaxSize()) { ground, size ->
                OnGround(ground, size) { m ->
                    CarPetCharacter(species, state.mood.emotion, state.stage, m, sizeRatio = state.growth.sizeRatio)
                }
                HeadBubble(state.mood.line, ground, size, state.growth.sizeRatio)
            }
        },
        info = { InfoColumn(state, species, onSuggestion, Modifier.fillMaxWidth()) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoColumn(state: CarPetUiState, species: Species, onSuggestion: (ActionKind) -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.block)) {
        // Who
        RarityTag(species)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(state.profile.name.orEmpty(), style = Type.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(metaLine(state), style = Type.meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // What grew: today, all together, height
        StatRow(
            listOf(
                "오늘 달린 거리" to (state.todayKm?.let { formatKm(it) } ?: "—"),
                "누적 주행거리" to (state.signals.odometerKm?.let { formatKm(it) } ?: "—"),
                "키" to "${Lines.heightText(state.growth.heightCm)} cm",
            ),
        )

        // The next birthday or stage
        val goal = state.growth.nextGoal
        Column(verticalArrangement = Arrangement.spacedBy(Space.tight)) {
            GrowthBar(goal.progress)
            Text("${formatKm(goal.kmLeft)} 더 달리면 ${goal.label.withSubject()} 돼요", style = Type.caption)
        }

        // The body as the friend feels it; what is low is marked, so it also says why the line
        // above the head is worried. The line itself is in the bubble over the friend.
        val chips = statusChips(state)
        if (chips.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.tight)) {
                Text("지금 내 몸 상태", style = Type.label)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.tight), verticalArrangement = Arrangement.spacedBy(Space.tight)) {
                    chips.forEach { (text, alert) -> InfoChip(text, alert) }
                }
            }
        }

        // One action
        state.mood.suggestion?.let { s ->
            PrimaryButton(text = s.label, onClick = { onSuggestion(s.kind) })
        }
        Text(sourceText(state), style = Type.caption)
    }
}

private fun metaLine(state: CarPetUiState): String {
    val parts = mutableListOf(state.stage.label, state.growth.ageLabel)
    state.profile.hatchedAtMillis?.let { hatched ->
        val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - hatched) + 1
        parts += "함께한 지 ${days}일"
    }
    return parts.joinToString(" · ")
}

/** The body as the friend feels it: only what keeps it healthy, and only values this car reports. */
private fun statusChips(state: CarPetUiState): List<Pair<String, Boolean>> {
    val s = state.signals
    fun fluid(name: String, level: FluidLevel): Pair<String, Boolean>? = when (level) {
        FluidLevel.NORMAL -> "$name 정상" to false
        FluidLevel.LOW -> "$name 부족" to true
        FluidLevel.CRITICALLY_LOW -> "$name 매우 부족" to true
        FluidLevel.UNKNOWN -> null
    }
    return listOfNotNull(
        s.batteryPercent?.let { "배터리 $it%" to (it <= EmotionEngine.HUNGRY_PERCENT) },
        fluid("엔진오일", s.engineOil),
        fluid("브레이크액", s.brakeFluid),
        s.washerFluidLow?.let { if (it) "워셔액 부족" to true else "워셔액 정상" to false },
        s.powerDownWarning?.let { if (it) "출력 저하" to true else null },
    )
}

private fun sourceText(state: CarPetUiState): String {
    val base = when (state.sourceStatus) {
        SourceStatus.Connecting -> "몸 상태를 읽는 중"
        SourceStatus.Connected -> "몸 상태 실시간 연결됨"
        is SourceStatus.Unavailable -> "몸 상태를 읽지 못했어요"
    }
    return if (state.overrides.isActive) "$base · 시연 값 사용 중" else base
}

internal fun formatKm(km: Float): String = NumberFormat.getIntegerInstance().format(km.toLong().coerceAtLeast(0)) + " km"
