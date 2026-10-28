package com.carpet.app.vehicle

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.OilLevelState
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.LowWasherFluidWaringListener
import ai.pleos.playground.vehicle.listener.OilLevelStateListener
import ai.pleos.playground.vehicle.listener.PowerDownWarningIndicatorListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import ai.pleos.playground.vehicle.listener.WiperStateListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Reads vehicle state through the Pleos Vehicle SDK. Only normal-level CAR_* permissions are used;
 * telltales and tire pressure need PRIVATE_CAR_SAFETY and are out of reach for third-party apps.
 */
class PleosVehicleSource(context: Context) : VehicleSource {

    private val appContext = context.applicationContext
    private val _signals = MutableStateFlow(VehicleSignals())
    private val _status = MutableStateFlow<SourceStatus>(SourceStatus.Connecting)
    private val _failures = MutableStateFlow<Map<String, String>>(emptyMap())

    override val signals: StateFlow<VehicleSignals> = _signals.asStateFlow()
    override val status: StateFlow<SourceStatus> = _status.asStateFlow()
    override val failures: StateFlow<Map<String, String>> = _failures.asStateFlow()

    private var vehicle: Vehicle? = null
    private val unregisterActions = mutableListOf<() -> Unit>()

    // SDK calls can block for seconds (domain lookup, binder calls), so they run on their own
    // looper thread instead of the main thread. start/stop are serialized on it.
    private val thread = HandlerThread("carpet-vehicle").apply { start() }
    private val handler = Handler(thread.looper)

    private val seatAreas = mapOf(
        Seat.DRIVER to SeatValue.ROW_1_LEFT,
        Seat.PASSENGER to SeatValue.ROW_1_RIGHT,
        Seat.REAR_LEFT to SeatValue.ROW_2_LEFT,
        Seat.REAR_RIGHT to SeatValue.ROW_2_RIGHT,
    )

    override fun start() {
        handler.post { connect() }
    }

    override fun stop() {
        handler.post {
            disconnect()
            thread.quitSafely()
        }
    }

    private fun connect() {
        if (vehicle != null) return
        val v = try {
            Vehicle(appContext).also { it.initialize() }
        } catch (t: Throwable) {
            // Not a Pleos Connect device (or Vehicle Service missing).
            Log.w(TAG, "Vehicle SDK unavailable", t)
            _status.value = SourceStatus.Unavailable(t.message ?: t.javaClass.simpleName)
            return
        }
        vehicle = v
        subscribeAll(v)
        // initialize() succeeds even without Pleos Vehicle Service; every call then throws
        // SecurityException. Treat "nothing subscribed" as unavailable.
        _status.value = if (subscribed > 0) {
            SourceStatus.Connected
        } else {
            SourceStatus.Unavailable("차량 서비스에 연결하지 못함")
        }
    }

    private var subscribed = 0

    private fun subscribeAll(v: Vehicle) {
        subscribe("battery") {
            val api = v.getEvBattery()
            val listener = object : EvBatteryStateOfChargeListener {
                override fun onEvBatteryStateOfChargeUpdated(value: Int?) {
                    _signals.update { it.copy(batteryPercent = value) }
                }
                override fun onFailed(e: Exception) = fail("battery", e)
            }
            api.getEvBatteryStateOfCharge(
                onComplete = { value -> _signals.update { it.copy(batteryPercent = value) } },
                onFailed = { e -> fail("battery", e) },
            )
            api.registerEvBatteryStateOfCharge(listener)
            unregisterActions += { api.unregisterEvBatteryStateOfCharge(listener) }
        }

        subscribe("odometer") {
            val api = v.getOdometer()
            val listener = object : DistanceDrivenListener {
                override fun onDistanceDrivenUpdated(value: Float?) {
                    _signals.update { it.copy(odometerKm = value) }
                }
                override fun onFailed(e: Exception) = fail("odometer", e)
            }
            api.getCurrentDistanceDriven(
                onComplete = { value -> _signals.update { it.copy(odometerKm = value) } },
                onFailed = { e -> fail("odometer", e) },
            )
            api.registerDistanceDriven(listener)
            unregisterActions += { api.unregisterDistanceDriven(listener) }
        }

        for ((seat, value) in seatAreas) {
            val name = "seat.${seat.name.lowercase()}"
            subscribe(name) {
                val api = v.getSeat()
                val area = SeatArea(value)
                val listener = object : SeatOccupancyListener {
                    override fun onSeatOccupancyUpdated(occupancy: Boolean?) = setSeat(seat, occupancy)
                    override fun onFailed(e: Exception) = fail(name, e)
                }
                api.getCurrentSeatOccupancy(
                    area = area,
                    onComplete = { occupancy -> setSeat(seat, occupancy) },
                    onFailed = { e -> fail(name, e) },
                )
                api.registerSeatOccupancy(area, listener)
                unregisterActions += { api.unregisterSeatOccupancy(area, listener) }
            }
        }

        subscribe("wiper") {
            val api = v.getWiper()
            val area = WindowArea(WindowValue.FRONT_WINDSHIELD)
            val listener = object : WiperStateListener {
                override fun onWiperStateUpdated(state: WiperState?) {
                    _signals.update { it.copy(wiperOn = state?.let { s -> s == WiperState.ON }) }
                }
                override fun onFailed(e: Exception) = fail("wiper", e)
            }
            api.registerWiperState(area, listener)
            unregisterActions += { api.unregisterWiperState(area, listener) }
        }

        subscribe("brake") {
            val api = v.getBrake()
            val listener = object : BrakePedalStatusListener {
                override fun onBrakePedalStatusUpdated(status: Boolean?) {
                    _signals.update { it.copy(brakePressed = status) }
                }
                override fun onFailed(e: Exception) = fail("brake", e)
            }
            api.registerBrakePedalStatus(listener)
            unregisterActions += { api.unregisterBrakePedalStatus(listener) }
        }

        subscribe("engineOil") {
            val api = v.getSafety()
            val listener = object : OilLevelStateListener {
                override fun onOilLevelUpdated(state: OilLevelState?) {
                    _signals.update { it.copy(engineOil = state.toFluidLevel()) }
                }
                override fun onFailed(e: Exception) = fail("engineOil", e)
            }
            api.registerEngineOilLevelState(listener)
            unregisterActions += { api.unregisterEngineOilLevelState(listener) }
        }

        subscribe("brakeFluid") {
            val api = v.getSafety()
            val listener = object : OilLevelStateListener {
                override fun onOilLevelUpdated(state: OilLevelState?) {
                    _signals.update { it.copy(brakeFluid = state.toFluidLevel()) }
                }
                override fun onFailed(e: Exception) = fail("brakeFluid", e)
            }
            api.registerBrakeFluidLevelState(listener)
            unregisterActions += { api.unregisterBrakeFluidLevelState(listener) }
        }

        subscribe("washerFluid") {
            val api = v.getSafety()
            val listener = object : LowWasherFluidWaringListener {
                override fun onLowWasherFluidWaringUpdated(warning: Boolean?) {
                    _signals.update { it.copy(washerFluidLow = warning) }
                }
                override fun onFailed(e: Exception) = fail("washerFluid", e)
            }
            api.registerLowWasherFluidWaringState(listener)
            unregisterActions += { api.unregisterLowWasherFluidWaringState(listener) }
        }

        subscribe("powerDown") {
            val api = v.getSafety()
            val listener = object : PowerDownWarningIndicatorListener {
                override fun onPowerDownWarningIndicatorUpdated(warning: Boolean?) {
                    _signals.update { it.copy(powerDownWarning = warning) }
                }
                override fun onFailed(e: Exception) = fail("powerDown", e)
            }
            api.registerPowerDownWarningIndicator(listener)
            unregisterActions += { api.unregisterPowerDownWarningIndicator(listener) }
        }
    }

    private fun disconnect() {
        unregisterActions.forEach { runCatching(it) }
        unregisterActions.clear()
        vehicle?.let { runCatching { it.release() } }
        vehicle = null
        subscribed = 0
    }

    /** One unsupported domain must not stop the others from subscribing. */
    private inline fun subscribe(name: String, block: () -> Unit) {
        try {
            block()
            subscribed++
        } catch (t: Throwable) {
            fail(name, t)
        }
    }

    private fun setSeat(seat: Seat, occupancy: Boolean?) {
        _signals.update { s ->
            s.copy(seats = if (occupancy == null) s.seats - seat else s.seats + (seat to occupancy))
        }
    }

    private fun fail(name: String, t: Throwable) {
        Log.w(TAG, "$name failed", t)
        _failures.update { it + (name to (t.message ?: t.javaClass.simpleName)) }
    }

    private fun OilLevelState?.toFluidLevel(): FluidLevel = when (this) {
        OilLevelState.NORMAL, OilLevelState.HIGH -> FluidLevel.NORMAL
        OilLevelState.LOW -> FluidLevel.LOW
        OilLevelState.CRITICALLY_LOW -> FluidLevel.CRITICALLY_LOW
        else -> FluidLevel.UNKNOWN
    }

    private companion object {
        const val TAG = "CarPet.Vehicle"
    }
}
