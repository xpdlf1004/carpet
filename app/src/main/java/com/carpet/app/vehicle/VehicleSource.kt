package com.carpet.app.vehicle

import kotlinx.coroutines.flow.StateFlow

sealed interface SourceStatus {
    data object Connecting : SourceStatus
    data object Connected : SourceStatus
    data class Unavailable(val reason: String) : SourceStatus
}

interface VehicleSource {
    val signals: StateFlow<VehicleSignals>
    val status: StateFlow<SourceStatus>

    /** Signal name -> last error. Shown in the demo panel to debug unsupported areas or permissions. */
    val failures: StateFlow<Map<String, String>>

    fun start()
    fun stop()
}
