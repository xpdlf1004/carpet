package com.carpet.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carpet.app.data.PetProfile
import com.carpet.app.data.PetProfileStore
import com.carpet.app.data.RestingFriend
import com.carpet.app.domain.ActionKind
import com.carpet.app.domain.Celebration
import com.carpet.app.domain.CelebrationTracker
import com.carpet.app.domain.DriveDay
import com.carpet.app.domain.DriveLog
import com.carpet.app.domain.EmotionEngine
import com.carpet.app.domain.Growth
import com.carpet.app.domain.GrowthStage
import com.carpet.app.domain.LineContext
import com.carpet.app.domain.Milestone
import com.carpet.app.domain.Mood
import com.carpet.app.domain.Species
import com.carpet.app.vehicle.PleosNavi
import com.carpet.app.vehicle.PleosVehicleSource
import com.carpet.app.vehicle.SignalOverrides
import com.carpet.app.vehicle.SourceStatus
import com.carpet.app.vehicle.VehicleSignals
import com.carpet.app.vehicle.VehicleSource
import com.carpet.app.vehicle.withOverrides
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

data class CarPetUiState(
    val profile: PetProfile = PetProfile(),
    val species: Species? = null,
    val signals: VehicleSignals = VehicleSignals(),
    val growth: Growth = Growth(0f),
    val todayKm: Float? = null,
    val mood: Mood = EmotionEngine.decide(VehicleSignals(), null, 0L),
    val resting: List<RestingFriend> = emptyList(),
    val sourceStatus: SourceStatus = SourceStatus.Connecting,
    val overrides: SignalOverrides = SignalOverrides(),
    val failures: Map<String, String> = emptyMap(),
) {
    val stage: GrowthStage get() = growth.stage
}

private data class SourceInfo(
    val status: SourceStatus,
    val overrides: SignalOverrides,
    val failures: Map<String, String>,
)

private data class Friends(val profile: PetProfile, val resting: List<RestingFriend>, val drive: DriveLog)

private data class Boarding(val atMillis: Long, val daysAway: Int?)

class CarPetViewModel(app: Application) : AndroidViewModel(app) {

    private val source: VehicleSource = PleosVehicleSource(app)
    private val navi = PleosNavi(app)
    private val store = PetProfileStore(app)
    private val overrides = MutableStateFlow(SignalOverrides())
    private val boarding = MutableStateFlow<Boarding?>(null)
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    // Declared before init {} runs watchCelebrations().
    private val tracker = CelebrationTracker()
    private val _celebration = MutableStateFlow<Celebration?>(null)
    /** A birthday or a new stage waiting to be shown; cleared by [dismissCelebration]. */
    val celebration: StateFlow<Celebration?> = _celebration.asStateFlow()

    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000)
        }
    }

    private val effectiveSignals = combine(source.signals, overrides) { s, o -> s.withOverrides(o) }

    private val sourceInfo = combine(source.status, overrides, source.failures, ::SourceInfo)

    private val friends = combine(store.profile, store.resting, store.drive, ::Friends)

    val uiState: StateFlow<CarPetUiState> =
        combine(effectiveSignals, friends, boarding, clock, sourceInfo) { signals, f, board, now, info ->
            // Until the SDK answers, show the last known odometer so the friend does not flash as a baby.
            val growth = Growth(signals.odometerKm ?: f.drive.lastOdometerKm ?: 0f)
            val today = LocalDate.now().toEpochDay()
            val todayKm = info.overrides.todayKm ?: DriveDay.todayKm(f.drive, signals.odometerKm, today)
            val time = LocalDateTime.now()
            val context = LineContext(
                hour = time.hour,
                dayOfWeek = time.dayOfWeek.value,
                month = time.monthValue,
                todayKm = todayKm,
                growth = growth,
                daysAway = board?.daysAway,
            )
            CarPetUiState(
                profile = f.profile,
                species = Species.fromKey(f.profile.speciesKey),
                signals = signals,
                growth = growth,
                todayKm = todayKm,
                mood = EmotionEngine.decide(signals, board?.atMillis, now, context),
                resting = f.resting,
                sourceStatus = info.status,
                overrides = info.overrides,
                failures = info.failures,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), startState())

    /** The saved friend from the first frame, so a cold start does not flash the egg screen. */
    private fun startState(): CarPetUiState {
        val p = store.profile.value
        return CarPetUiState(
            profile = p,
            species = Species.fromKey(p.speciesKey),
            growth = Growth(store.drive.value.lastOdometerKm ?: 0f),
            resting = store.resting.value,
        )
    }

    init {
        source.start()
        navi.start()

        // A driver sitting down (empty -> occupied) starts the greeting window.
        viewModelScope.launch {
            effectiveSignals.map { it.driverSeated }.distinctUntilChanged().collect { seated ->
                if (seated) board()
            }
        }

        // Today's km and the last drive day come from the real odometer only, never the demo values.
        viewModelScope.launch {
            source.signals.map { it.odometerKm }.filterNotNull().distinctUntilChanged().collect { km ->
                store.recordDrive(DriveDay.accept(store.drive.value, km, LocalDate.now().toEpochDay()))
            }
        }

        watchCelebrations()
    }

    /**
     * Each birthday and stage is celebrated once. Real growth is saved; demo values only show the
     * moment, so a 15만 km preset neither uses up nor blocks the real celebrations.
     */
    private fun watchCelebrations() {
        viewModelScope.launch {
            combine(effectiveSignals.map { it.odometerKm }, store.profile, overrides.map { it.odometerKm != null }) { km, p, demo ->
                Triple(km, p, demo)
            }.distinctUntilChanged().collect { (km, profile, demo) ->
                if (km == null || profile.speciesKey == null || profile.name == null) {
                    tracker.forget()
                    return@collect
                }
                val g = Growth(km)
                val saved = Milestone(profile.celebratedAge, profile.celebratedStage)
                tracker.observe(g, demo, saved)?.let { _celebration.value = it }
                if (!demo && Milestone.of(g) > saved) store.markCelebrated(g.ageYears, g.stage.ordinal)
            }
        }
    }

    private fun board() {
        val today = LocalDate.now().toEpochDay()
        boarding.value = Boarding(System.currentTimeMillis(), DriveDay.daysAway(store.drive.value, today))
    }

    /** The egg hatches with the given name; the species is drawn here. Returns the friend. */
    fun hatch(name: String): Species {
        val species = Species.draw()
        // The replay and the screen use the shown km (a demo preset counts); the celebration
        // record uses the real odometer, so real birthdays still come.
        val shownKm = uiState.value.signals.odometerKm
        val real = Growth(source.signals.value.odometerKm ?: 0f)
        tracker.startAt(Growth(shownKm ?: 0f), demo = overrides.value.odometerKm != null)
        store.hatch(species.key, name.trim(), System.currentTimeMillis(), shownKm, real.ageYears, real.stage.ordinal)
        _celebration.value = null
        board()
        return species
    }

    fun dismissCelebration() {
        _celebration.value = null
    }

    /**
     * A new body (new car): the odometer starts near 0, so the current friend rests in the friend
     * box with the km it reached and a new egg waits. In the demo the new body is the 0 km preset.
     */
    fun moveToNewBody() {
        val km = uiState.value.signals.odometerKm ?: 0f
        store.rest(km, System.currentTimeMillis())
        overrides.update { it.copy(odometerKm = 0f, todayKm = 0f) }
        _celebration.value = null
    }

    /** Demo panel: switch character without drawing again (keeps the name). */
    fun pickSpecies(species: Species) = store.setSpecies(species.key)

    /** Demo panel: everything back to the egg, friend box included. */
    fun resetPet() {
        store.clearAll()
        _celebration.value = null
    }

    fun updateOverrides(transform: (SignalOverrides) -> SignalOverrides) = overrides.update(transform)

    /** Demo panel: drive [km] more, so today's km and the age move together. */
    fun demoDrive(km: Float) {
        val s = uiState.value
        val odometer = (s.signals.odometerKm ?: 0f) + km
        val today = (s.todayKm ?: 0f) + km
        overrides.update { it.copy(odometerKm = odometer, todayKm = today) }
    }

    fun clearOverrides() {
        overrides.value = SignalOverrides()
    }

    /** Opens the Pleos nav on nearby chargers / repair shops; the driver picks the stop there. */
    fun requestWaypoint(kind: ActionKind) {
        val target = when (kind) {
            ActionKind.CHARGER -> "충전소"
            ActionKind.REPAIR_SHOP -> "정비소"
        }
        val opened = navi.showNearby(kind)
        _messages.tryEmit(
            if (opened) "내비에 가까운 ${target}를 띄웠어요. 들를 곳을 골라 주세요."
            else "내비를 열지 못했어요. 잠시 뒤에 다시 눌러 주세요.",
        )
    }

    override fun onCleared() {
        source.stop()
        navi.stop()
    }
}
