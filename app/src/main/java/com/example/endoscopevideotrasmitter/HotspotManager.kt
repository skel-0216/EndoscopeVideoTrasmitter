package com.example.endoscopevideotrasmitter
import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log

class HotspotManager(private val context: Context) {
    private var mReservation: WifiManager.LocalOnlyHotspotReservation? = null
    private val TAG = "HotspotManager"

    fun turnOnHotspot(ssid: String, password: String) {
        val manager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val wifiConfiguration = WifiConfiguration()
        wifiConfiguration.SSID = ssid
        wifiConfiguration.preSharedKey = password
        wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)

        manager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
            override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                super.onStarted(reservation)
                Log.d(TAG, "Wifi Hotspot is on now. SSID: $ssid, Password: $password")
                mReservation = reservation
            }

            override fun onStopped() {
                super.onStopped()
                Log.d(TAG, "onStopped: Wifi Hotspot is stopped")
            }

            override fun onFailed(reason: Int) {
                super.onFailed(reason)
                Log.d(TAG, "onFailed: Failed to start Wifi Hotspot")
            }
        }, Handler(Looper.getMainLooper()))
    }

    fun turnOffHotspot() {
        mReservation?.close()
    }
}