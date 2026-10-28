package com.carpet.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carpet.app.ui.theme.Palette
import com.carpet.app.ui.theme.Type
import com.carpet.app.domain.Species
import com.carpet.app.vehicle.FluidLevel
import com.carpet.app.vehicle.SignalOverrides

private const val DRIVE_STEP_KM = 150f
private const val BIG_DRIVE_KM = 2_000f
private val ODOMETER_PRESETS = listOf(0 to "0", 10_000 to "1만", 50_000 to "5만", 100_000 to "10만", 150_000 to "15만")

/**
 * Overrides vehicle signals for demos. Engine oil / brake fluid / washer fluid have no
 * Virtual Console card, so this panel is the only way to show 「아픔」 on the emulator.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DemoPanel(
    state: CarPetUiState,
    onClose: () -> Unit,
    onChange: ((SignalOverrides) -> SignalOverrides) -> Unit,
    onClear: () -> Unit,
    onResetPet: () -> Unit,
    onPickSpecies: (Species) -> Unit,
    onDrive: (Float) -> Unit,
    onNewBody: () -> Unit,
) {
    val o = state.overrides
    val s = state.signals
    Row(Modifier.fillMaxHeight()) {
        Box(Modifier.width(1.dp).fillMaxHeight().background(Palette.Hairline))
        Column(
            Modifier
                .width(380.dp)
                .fillMaxHeight()
                .background(Palette.White)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f))
                IconPill(CarPetIcons.Close, "닫기", onClose)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("시연 패널", style = Type.title.copy(fontSize = Type.name.fontSize * 0.6f, lineHeight = Type.name.lineHeight * 0.6f))
                Text("차량 신호를 덮어씁니다. 실제 차에서는 Vehicle SDK 값을 씁니다.".keepWordsTogether(), style = Type.caption)
            }

            Section("캐릭터") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (sp in Species.entries) {
                        ChoiceChip(sp.label, selected = state.species == sp, onClick = { onPickSpecies(sp) })
                    }
                }
            }

            Section("배터리") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = (s.batteryPercent ?: 80).toFloat(),
                        onValueChange = { v -> onChange { it.copy(batteryPercent = v.toInt()) } },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Palette.Graphite,
                            activeTrackColor = Palette.Graphite,
                            inactiveTrackColor = Palette.Mist,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Text("${s.batteryPercent ?: "—"}%", style = Type.chip, modifier = Modifier.padding(start = 12.dp).width(48.dp))
                }
            }

            Section("누적 주행거리 (km)") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((km, label) in ODOMETER_PRESETS) {
                        ChoiceChip(label, selected = o.odometerKm == km.toFloat(), onClick = { onChange { it.copy(odometerKm = km.toFloat(), todayKm = 0f) } })
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostPill("${DRIVE_STEP_KM.toInt()} km 달리기", onClick = { onDrive(DRIVE_STEP_KM) })
                    GhostPill("${BIG_DRIVE_KM.toInt()} km 달리기", onClick = { onDrive(BIG_DRIVE_KM) })
                    GhostPill("새 차로 바꾸기", onClick = onNewBody)
                }
            }

            Section("차량 상태") {
                ToggleRow("운전석 착석", s.driverSeated) { v -> onChange { it.copy(driverSeated = v) } }
                ToggleRow("와이퍼 작동", s.wiperOn == true) { v -> onChange { it.copy(wiperOn = v) } }
                ToggleRow("엔진오일 부족", s.engineOil == FluidLevel.LOW || s.engineOil == FluidLevel.CRITICALLY_LOW) { v ->
                    onChange { it.copy(engineOil = if (v) FluidLevel.LOW else FluidLevel.NORMAL) }
                }
                ToggleRow("브레이크액 부족", s.brakeFluid == FluidLevel.LOW || s.brakeFluid == FluidLevel.CRITICALLY_LOW) { v ->
                    onChange { it.copy(brakeFluid = if (v) FluidLevel.LOW else FluidLevel.NORMAL) }
                }
                ToggleRow("워셔액 부족", s.washerFluidLow == true) { v -> onChange { it.copy(washerFluidLow = v) } }
            }

            HorizontalDivider(color = Palette.Hairline)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostPill("시연 값 지우기", onClick = onClear)
                GhostPill("처음부터 다시", onClick = onResetPet)
            }

            if (state.failures.isNotEmpty()) {
                Section("SDK 오류") {
                    for ((name, message) in state.failures) Text("$name: $message", style = Type.caption)
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = Type.label)
        content()
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Type.body, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Palette.Graphite,
                checkedThumbColor = Palette.White,
                uncheckedTrackColor = Palette.Mist,
                uncheckedThumbColor = Palette.Faint,
                uncheckedBorderColor = Palette.Hairline,
            ),
        )
    }
}
