package com.carpet.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carpet.app.domain.Species
import com.carpet.app.ui.theme.Palette
import com.carpet.app.ui.theme.Type
import kotlinx.coroutines.delay

/** Egg shakes, cracks with light, then the drawn friend appears. */
private const val SHAKE_MILLIS = 900L
private const val CRACK_MILLIS = 1_100L

@Composable
fun CarPetApp(vm: CarPetViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var demoOpen by rememberSaveable { mutableStateOf(false) }
    var eggPhase by rememberSaveable { mutableStateOf(EggPhase.IDLE) }
    var pendingName by rememberSaveable { mutableStateOf<String?>(null) }
    var justHatched by rememberSaveable { mutableStateOf(false) }
    var friendsOpen by rememberSaveable { mutableStateOf(false) }
    var farewell by rememberSaveable { mutableStateOf(false) }
    val celebration by vm.celebration.collectAsStateWithLifecycle()

    LaunchedEffect(vm) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(eggPhase) {
        when (eggPhase) {
            EggPhase.SHAKING -> {
                delay(SHAKE_MILLIS)
                eggPhase = EggPhase.CRACKING
            }
            EggPhase.CRACKING -> {
                delay(CRACK_MILLIS)
                vm.hatch(pendingName.orEmpty())
                pendingName = null
                justHatched = true
                // Keep 「곧 나와요」 on the egg screen while it fades out.
                delay(600)
                eggPhase = EggPhase.IDLE
            }
            EggPhase.IDLE -> Unit
        }
    }

    Scaffold(
        containerColor = Palette.White,
        // The Pleos app pane already sits clear of the dock and status bar; extra insets only
        // made the bottom margin twice the top one.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    shape = RoundedCornerShape(20.dp),
                    containerColor = Palette.Graphite,
                    contentColor = Palette.White,
                    modifier = Modifier.padding(16.dp),
                ) { Text(data.visuals.message, style = Type.body.copy(color = Palette.White)) }
            }
        },
    ) { padding ->
        // The demo panel sits beside the screen (not over it) so the character and its line
        // stay visible while recording a demo.
        Row(
            Modifier
                .fillMaxSize()
                .background(Palette.White)
                .padding(padding),
        ) {
            // The demo panel is for judges and recordings, not drivers: a long press on the
            // top-left corner opens it (see README). Taps there still reach the screen below.
            Box(Modifier.weight(1f).fillMaxHeight().cornerLongPress { demoOpen = !demoOpen }) {
                val hatched = state.species != null && state.profile.name != null
                val step = when {
                    friendsOpen -> Step.FRIENDS
                    !hatched -> Step.EGG
                    justHatched -> Step.HELLO
                    farewell -> Step.FAREWELL
                    celebration != null -> Step.CELEBRATE
                    else -> Step.HOME
                }
                Crossfade(targetState = step, animationSpec = tween(450), label = "screen") { s ->
                    val species = state.species ?: Species.FOX
                    val name = state.profile.name.orEmpty()
                    when (s) {
                        Step.EGG -> EggScreen(phase = eggPhase, onName = { n ->
                            if (eggPhase == EggPhase.IDLE) {
                                pendingName = n
                                eggPhase = EggPhase.SHAKING
                            }
                        })
                        Step.HELLO -> HelloScreen(species, name, state.growth, onStart = { justHatched = false })
                        Step.HOME -> HomeScreen(state = state, onSuggestion = vm::requestWaypoint)
                        Step.CELEBRATE -> celebration?.let { CelebrationScreen(it, species, name, onDone = vm::dismissCelebration) }
                        Step.FAREWELL -> FarewellScreen(
                            species,
                            name,
                            state.growth,
                            onMove = {
                                vm.moveToNewBody()
                                farewell = false
                            },
                            onStay = { farewell = false },
                        )
                        Step.FRIENDS -> FriendsScreen(state, onClose = { friendsOpen = false })
                    }
                }
                if (step == Step.HOME || (step == Step.EGG && state.resting.isNotEmpty())) {
                    IconPill(
                        CarPetIcons.Archive,
                        "카펫함",
                        onClick = { friendsOpen = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 16.dp, end = 20.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = demoOpen,
                enter = expandHorizontally(expandFrom = Alignment.Start),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start),
            ) {
                DemoPanel(
                    state = state,
                    onClose = { demoOpen = false },
                    onChange = vm::updateOverrides,
                    onClear = vm::clearOverrides,
                    onResetPet = {
                        vm.resetPet()
                        vm.clearOverrides()
                        justHatched = false
                        farewell = false
                        friendsOpen = false
                    },
                    onPickSpecies = vm::pickSpecies,
                    onDrive = vm::demoDrive,
                    onNewBody = {
                        friendsOpen = false
                        farewell = true
                    },
                )
            }
        }
    }
}

private enum class Step { EGG, HELLO, HOME, CELEBRATE, FAREWELL, FRIENDS }

/** Long press in the top-left corner, watched without consuming, so taps there still work. */
private fun Modifier.cornerLongPress(onLongPress: () -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val corner = 120.dp.toPx()
        if (down.position.x > corner || down.position.y > corner) return@awaitEachGesture
        val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            waitForUpOrCancellation(pass = PointerEventPass.Initial)
        }
        if (released == null) onLongPress()
    }
}
