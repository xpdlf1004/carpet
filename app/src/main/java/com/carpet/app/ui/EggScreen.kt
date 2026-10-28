package com.carpet.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.carpet.app.domain.Rarity
import com.carpet.app.domain.Species
import com.carpet.app.ui.theme.Palette
import com.carpet.app.ui.theme.Space
import com.carpet.app.ui.theme.Type

private const val MAX_NAME = 8
private val SUGGESTIONS = listOf("콩이", "보리", "토리", "모카")

/**
 * First boarding. The egg asks for a name; naming it wakes the egg, and whoever hatches
 * becomes the car. Suggested names keep in-car typing short; the odds are one tap away.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EggScreen(phase: EggPhase, onName: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var showOdds by rememberSaveable { mutableStateOf(false) }
    val busy = phase != EggPhase.IDLE
    val submit = { if (name.isNotBlank() && !busy) onName(name.trim()) }

    TwoPane(
        stage = {
            StageCard(Scene.NURSERY, Modifier.fillMaxSize()) { ground, size ->
                OnGround(ground, size) { m -> EggView(phase, m) }
            }
        },
        info = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.block)) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.tight)) {
                    // The egg is talking. The title never changes, so nothing jumps while hatching.
                    Text("저에게 이름을\n지어 주세요", style = Type.title)
                    Text("이름을 지어 주면 알에서 나올게요.\n그다음부터 함께 달려요.", style = Type.lead)
                }

                NameField(name = name, enabled = !busy, onName = { name = it.take(MAX_NAME) }, onSubmit = submit)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.tight), verticalArrangement = Arrangement.spacedBy(Space.tight)) {
                    SUGGESTIONS.forEach { s -> ChoiceChip(text = s, selected = name == s, onClick = { if (!busy) name = s }) }
                }

                PrimaryButton(
                    text = when (phase) {
                        EggPhase.IDLE -> "이 이름으로 깨우기"
                        EggPhase.SHAKING -> "알이 움직여요"
                        EggPhase.CRACKING -> "곧 나와요"
                    },
                    onClick = submit,
                    enabled = name.isNotBlank() && !busy,
                )

                Column {
                    Row(
                        Modifier
                            .heightIn(min = 44.dp)
                            .clickable(role = Role.Button) { showOdds = !showOdds },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("다섯 카펫 중 하나가 태어나요", style = Type.caption)
                        Text(if (showOdds) "  확률 닫기" else "  확률 보기", style = Type.caption.copy(color = Palette.Body))
                    }
                    AnimatedVisibility(visible = showOdds) { OddsTable() }
                }
            }
        },
    )
}

@Composable
private fun OddsTable() {
    Column(Modifier.fillMaxWidth()) {
        Species.entries.forEachIndexed { i, s ->
            if (i > 0) HorizontalDivider(color = Palette.Mist)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(s.label, style = Type.body.copy(color = Palette.Ink), modifier = Modifier.weight(1f))
                Text(
                    s.rarity.label,
                    style = Type.chip.copy(color = if (s.rarity == Rarity.COMMON) Palette.Slate else Palette.Fox),
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(56.dp),
                )
                Text("${s.chancePercent}%", style = Type.body, textAlign = TextAlign.End, modifier = Modifier.width(64.dp))
            }
        }
    }
}

@Composable
private fun NameField(name: String, enabled: Boolean, onName: (String) -> Unit, onSubmit: () -> Unit) {
    var focused by rememberSaveable { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Palette.White,
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Palette.Graphite else Palette.Hairline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (name.isEmpty()) Text("이름 (최대 ${MAX_NAME}자)", style = Type.speech.copy(color = Palette.Faint), maxLines = 1)
                BasicTextField(
                    value = name,
                    onValueChange = onName,
                    enabled = enabled,
                    singleLine = true,
                    textStyle = Type.speech,
                    cursorBrush = SolidColor(Palette.Graphite),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused },
                )
            }
            Text("${name.length}/$MAX_NAME", style = Type.caption, modifier = Modifier.padding(start = 12.dp))
        }
    }
}
