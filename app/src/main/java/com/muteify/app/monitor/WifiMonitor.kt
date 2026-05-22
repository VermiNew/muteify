package com.muteify.app.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.muteify.app.data.model.TriggerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WifiMonitor(private val context: Context) {

    private val _state = MutableStateFlow(TriggerState.UNKNOWN)
    val state: StateFlow<TriggerState> = _state

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiPresenceChecker = WifiPresenceChecker(context)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            capabilities: NetworkCapabilities
        ) {
            val ssid = wifiPresenceChecker.currentSsid()
            updateState(ssid)
        }

        override fun onLost(network: Network) {
            _state.value = TriggerState.AWAY
        }
    }

    fun start(targetSsid: String) {
        this.targetSsid = targetSsid
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        // Check current state immediately
        updateState(wifiPresenceChecker.currentSsid())
    }

    fun stop() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: IllegalArgumentException) {
            // Not registered
        }
    }

    private var targetSsid: String = ""

    private fun updateState(currentSsid: String?) {
        _state.value = if (currentSsid != null && currentSsid == targetSsid) {
            TriggerState.HOME
        } else {
            TriggerState.AWAY
        }
    }
}
