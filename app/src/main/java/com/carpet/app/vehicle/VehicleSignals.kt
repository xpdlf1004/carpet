package com.carpet.app.vehicle

enum class FluidLevel { NORMAL, LOW, CRITICALLY_LOW, UNKNOWN }

enum class Seat { DRIVER, PASSENGER, REAR_LEFT, REAR_RIGHT }

/** Snapshot of the vehicle signals CarPet cares about. null = not received (yet) or unsupported. */
data class VehicleSignals(
    val batteryPercent: Int? = null,
    val odometerKm: Float? = null,
    val seats: Map<Seat, Boolean> = emptyMap(),
    val wiperOn: Boolean? = null,
    val brakePressed: Boolean? = null,
    val engineOil: FluidLevel = FluidLevel.UNKNOWN,
    val brakeFluid: FluidLevel = FluidLevel.UNKNOWN,
    val washerFluidLow: Boolean? = null,
    val powerDownWarning: Boolean? = null,
) {
    val driverSeated: Boolean get() = seats[Seat.DRIVER] == true
    val companionCount: Int get() = seats.count { (seat, occupied) -> seat != Seat.DRIVER && occupied }
}

/**
 * Values set from the in-app demo panel. Used for signals the Virtual Console cannot produce
 * (engine oil, brake fluid, washer fluid) and for quick testing without the console.
 */
data class SignalOverrides(
    val batteryPercent: Int? = null,
    val odometerKm: Float? = null,
    val driverSeated: Boolean? = null,
    val wiperOn: Boolean? = null,
    val engineOil: FluidLevel? = null,
    val brakeFluid: FluidLevel? = null,
    val washerFluidLow: Boolean? = null,
    /** Demo: km driven today, moved together with [odometerKm] by the 「달리기」 button. */
    val todayKm: Float? = null,
) {
    val isActive: Boolean get() = this != SignalOverrides()
}

fun VehicleSignals.withOverrides(o: SignalOverrides): VehicleSignals = copy(
    batteryPercent = o.batteryPercent ?: batteryPercent,
    odometerKm = o.odometerKm ?: odometerKm,
    seats = o.driverSeated?.let { seats + (Seat.DRIVER to it) } ?: seats,
    wiperOn = o.wiperOn ?: wiperOn,
    engineOil = o.engineOil ?: engineOil,
    brakeFluid = o.brakeFluid ?: brakeFluid,
    washerFluidLow = o.washerFluidLow ?: washerFluidLow,
)
