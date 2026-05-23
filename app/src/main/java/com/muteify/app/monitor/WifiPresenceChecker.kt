package com.muteify.app.monitor

import android.content.Context
import android.net.wifi.WifiManager

class WifiPresenceChecker(context: Context) {

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun currentSsid(): String? {
        return wifiManager.connectionInfo?.ssid?.toKnownSsidOrNull()
    }

    fun isConnectedTo(targetSsid: String): Boolean {
        return currentSsid() == targetSsid
    }

    private fun String.toKnownSsidOrNull(): String? {
        val ssid = removeSurrounding("\"")
        return ssid.takeUnless {
            it.isBlank() ||
                it == WifiManager.UNKNOWN_SSID ||
                it == "0x"
        }
    }
}
