package com.carpet.app.vehicle

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import ai.pleos.playground.navi.data.NavigationScreenRequest
import ai.pleos.playground.navi.helper.NaviHelper
import com.carpet.app.domain.ActionKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hands the "go there" action to the Pleos navigation app: it opens on nearby charging
 * stations, or on a search for repair shops, and the driver picks where to stop. The nav's
 * own place data is used, so CarPet ships no POI list.
 */
class PleosNavi(context: Context) {

    private val appContext = context.applicationContext
    private val thread = HandlerThread("carpet-navi").apply { start() }
    private val handler = Handler(thread.looper)
    private var navi: NaviHelper? = null

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    /** A request made before the nav service answered; sent as soon as it connects. */
    @Volatile private var pending: NavigationScreenRequest? = null

    fun start() {
        handler.post { connect() }
    }

    // Runs on [thread]. The nav service can come up after CarPet (e.g. right after boot), so a
    // failed or dropped connection is retried on the next request instead of giving up.
    private fun connect() {
        if (navi != null && _connected.value) return
        navi?.let { runCatching { it.release() } }
        try {
            navi = NaviHelper(appContext).also { helper ->
                helper.initialize(
                    onConnected = {
                        _connected.value = true
                        pending?.let { request ->
                            pending = null
                            handler.post { send(request) }
                        }
                    },
                    onFailed = { t ->
                        _connected.value = false
                        Log.w(TAG, "NaviHelper connection failed", t)
                    },
                )
            }
        } catch (t: Throwable) {
            navi = null
            Log.w(TAG, "NaviHelper unavailable", t)
        }
    }

    private fun send(request: NavigationScreenRequest): Boolean {
        val helper = navi ?: return false
        return try {
            helper.requestNavigationScreen(request)
            true
        } catch (t: Throwable) {
            Log.w(TAG, "requestNavigationScreen failed", t)
            _connected.value = false
            false
        }
    }

    /**
     * Opens the nav on what the character needs. When the nav service is not connected yet, the
     * request waits for the connection. Returns false only when the nav cannot be reached at all.
     */
    fun showNearby(kind: ActionKind): Boolean {
        val request = when (kind) {
            ActionKind.CHARGER -> NavigationScreenRequest.NearbyChargingStations
            ActionKind.REPAIR_SHOP -> NavigationScreenRequest.SearchPlace(REPAIR_KEYWORD)
        }
        if (_connected.value && send(request)) return true
        pending = request
        handler.post { connect() }
        return true
    }

    fun stop() {
        handler.post {
            navi?.let { runCatching { it.release() } }
            navi = null
            _connected.value = false
            thread.quitSafely()
        }
    }

    private companion object {
        const val TAG = "CarPet.Navi"
        const val REPAIR_KEYWORD = "자동차 정비"
    }
}
