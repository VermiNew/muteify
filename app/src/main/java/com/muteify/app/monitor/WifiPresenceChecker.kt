package com.muteify.app.monitor

import android.content.Context
import android.net.wifi.WifiManager

class WifiPresenceChecker(context: Context) {

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun currentSsid(): String? {
        return wifiManager.connectionInfo?.ssid?.removeSurrounding("\"")
    }

    fun isConnectedTo(targetSsid: String): Boolean {
        return currentSsid() == targetSsid
    }
}
