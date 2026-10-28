package com.carpet.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carpet.app.domain.Emotion
import com.carpet.app.domain.Growth
import com.carpet.app.domain.Species
import com.carpet.app.ui.theme.Palette
import com.carpet.app.ui.theme.Space
import com.carpet.app.ui.theme.Type

private data class FriendCard(val species: Species, val name: String, val growth: Growth, val now: Boolean)

/**
 * The friend box: the friend riding now and the ones resting from earlier bodies, then the five
 * kinds of friends with the ones not met yet as silhouettes. Friends keep the km they reached.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FriendsScreen(state: CarPetUiState, onClose: () -> Unit) {
    val cards = buildList {
        val current = state.species
        val name = state.profile.name
        if (current != null && name != null) add(FriendCard(current, name, state.growth, now = true))
        state.resting.asReversed().forEach { f ->
            Species.fromKey(f.speciesKey)?.let { add(FriendCard(it, f.name, Growth(f.finalKm), now = false)) }
        }
    }
    val met = cards.map { it.species }.toSet()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.screenH, vertical = Space.screenV),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("카펫함", style = Type.title, modifier = Modifier.weight(1f))
            IconPill(CarPetIcons.Close, "닫기", onClose)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.tight)) {
            Text(
                if (cards.isEmpty()) "아직 만난 카펫이 없어요. 알을 깨워 주세요." else "함께 달린 카펫 ${cards.size}명이 모여 있어요.",
                style = Type.lead,
            )
        }

        if (cards.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                cards.forEach { FriendTile(it) }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("다섯 카펫", style = Type.label)
                Text("  ${met.size} / ${Species.entries.size} 만남", style = Type.caption)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Species.entries.forEach { DexTile(it, it in met) }
            }
        }
    }
}

@Composable
private fun FriendTile(card: FriendCard) {
    Surface(shape = RoundedCornerShape(28.dp), color = Palette.Mist, modifier = Modifier.width(260.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1.5f), contentAlignment = Alignment.BottomCenter) {
                CarPetCharacter(
                    card.species,
                    if (card.now) Emotion.GLAD else Emotion.CALM,
                    card.growth.stage,
                    Modifier.size(150.dp),
                    sizeRatio = card.growth.sizeRatio,
                )
                StatusBadge(if (card.now) "지금 함께" else "쉬는 중", accent = card.now, modifier = Modifier.align(Alignment.TopStart))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(card.name, style = Type.speech.copy(color = Palette.Heading), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${card.species.label} · ${card.growth.stage.label} · ${card.growth.ageLabel}", style = Type.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("누적 ${formatKm(card.growth.odometerKm)}", style = Type.caption, maxLines = 1)
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String, accent: Boolean, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = if (accent) Palette.Fox else Palette.White, modifier = modifier) {
        Text(
            text,
            style = Type.caption.copy(color = if (accent) Palette.White else Palette.Body),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

/** One kind of friend; ones not met yet are a grey silhouette with 「?」. */
@Composable
private fun DexTile(species: Species, met: Boolean) {
    Column(Modifier.width(112.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(shape = RoundedCornerShape(24.dp), color = Palette.Mist, modifier = Modifier.size(112.dp)) {
            Box(contentAlignment = Alignment.BottomCenter) {
                CarPetCharacter(
                    species,
                    Emotion.CALM,
                    com.carpet.app.domain.GrowthStage.YOUTH,
                    Modifier
                        .size(96.dp)
                        .graphicsLayer { alpha = if (met) 1f else 0.9f },
                    colorFilter = if (met) null else ColorFilter.tint(Palette.Hairline),
                )
                if (!met) Text("?", style = Type.title.copy(color = Palette.Faint), modifier = Modifier.align(Alignment.Center))
            }
        }
        Text(if (met) species.label else "아직 못 만남", style = Type.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(species.rarity.label, style = Type.caption.copy(color = if (species.rarity == com.carpet.app.domain.Rarity.COMMON) Palette.Faint else Palette.Fox))
    }
}
