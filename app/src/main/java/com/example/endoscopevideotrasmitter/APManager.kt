package com.example.endoscopevideotrasmitter

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.lang.Exception
import java.lang.reflect.Method
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

class APManager private constructor(context: Context) {
    private val wifiManager: WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val locationManager: LocationManager? = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val utilities: Utils = Utils()

    var ssid: String? = null
        private set
    var passwordValue: String? = null
        private set
    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null

    fun getSSID(): String? {
        return ssid
    }

    fun getPassword(): String? {
        return passwordValue
    }

    fun getUtils(): Utils {
        return utilities
    }

    fun turnOnHotspot(
        context: Context,
        onSuccessListener: OnSuccessListener,
        onFailureListener: OnFailureListener
    ) {
        Log.d("CHECK=========", "${isWifiApEnabled()}")
        val providerEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false

        if (isDeviceConnectedToWifi()) {
            onFailureListener.onFailure(ERROR_DISABLE_WIFI, null)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (utilities.checkLocationPermission(context) && providerEnabled && !isWifiApEnabled()) {
                try {
                    wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                        override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                            super.onStarted(reservation)
                            this@APManager.reservation = reservation
                            try {
                                ssid = reservation.wifiConfiguration?.SSID
                                passwordValue = reservation.wifiConfiguration?.preSharedKey
                                onSuccessListener.onSuccess(ssid!!, passwordValue!!)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                onFailureListener.onFailure(ERROR_UNKNOWN, e)
                            }
                        }

                        override fun onFailed(reason: Int) {
                            super.onFailed(reason)
                            onFailureListener.onFailure(
                                if (reason == ERROR_TETHERING_DISALLOWED) ERROR_DISABLE_HOTSPOT else ERROR_UNKNOWN,
                                null
                            )
                        }
                    }, Handler(Looper.getMainLooper()))
                } catch (e: Exception) {
                    onFailureListener.onFailure(ERROR_UNKNOWN, e)
                }
            } else if (!providerEnabled) {
                onFailureListener.onFailure(ERROR_GPS_PROVIDER_DISABLED, null)
            } else if (isWifiApEnabled()) {
                onFailureListener.onFailure(ERROR_DISABLE_HOTSPOT, null)
            } else {
                onFailureListener.onFailure(ERROR_LOCATION_PERMISSION_DENIED, null)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!utilities.checkLocationPermission(context)) {
                    onFailureListener.onFailure(ERROR_LOCATION_PERMISSION_DENIED, null)
                    return
                }
                if (!utilities.checkWriteSettingPermission(context)) {
                    onFailureListener.onFailure(ERROR_WRITE_SETTINGS_PERMISSION_REQUIRED, null)
                    return
                }
            }
            try {
                ssid = "AndroidAP_" + Random().nextInt(10000)
                passwordValue = getRandomPassword()
                val wifiConfiguration = WifiConfiguration()
                wifiConfiguration.SSID = ssid
                wifiConfiguration.preSharedKey = passwordValue
                wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED)
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                wifiManager.isWifiEnabled = false
                setWifiApEnabled(wifiConfiguration, true)
                onSuccessListener.onSuccess(ssid!!, passwordValue!!)
            } catch (e: Exception) {
                e.printStackTrace()
                onFailureListener.onFailure(ERROR_LOCATION_PERMISSION_DENIED, e)
            }
        }
    }

    fun disableWifiAp() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                reservation?.close()
            } else {
                setWifiApEnabled(null, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isWifiApEnabled(): Boolean {
        return try {
            val method: Method = wifiManager.javaClass.getMethod("isWifiApEnabled")
            method.invoke(wifiManager) as Boolean
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isDeviceConnectedToWifi(): Boolean {
        return wifiManager.dhcpInfo.ipAddress != 0
    }

    @Throws(Exception::class)
    private fun setWifiApEnabled(wifiConfiguration: WifiConfiguration?, enable: Boolean) {
        val method: Method = wifiManager.javaClass.getMethod("setWifiApEnabled", WifiConfiguration::class.java, Boolean::class.javaPrimitiveType)
        method.invoke(wifiManager, wifiConfiguration, enable)
    }

    interface OnFailureListener {
        fun onFailure(failureCode: Int, e: Exception?)
    }

    interface OnSuccessListener {
        fun onSuccess(ssid: String, password: String)
    }

    private fun getRandomPassword(): String {
        return try {
            val ms: MessageDigest = MessageDigest.getInstance("MD5")
            val bytes = ByteArray(10)
            Random().nextBytes(bytes)
            val digest: ByteArray = ms.digest(bytes)
            val bigInteger = BigInteger(1, digest)
            bigInteger.toString(16).substring(0, 10)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            "jfs82433#$2"
        }
    }

    class Utils {
        fun checkLocationPermission(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        fun askLocationPermission(activity: Activity, requestCode: Int) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestCode)
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        fun askWriteSettingPermission(activity: Activity) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:" + activity.packageName)
            activity.startActivity(intent)
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        fun checkWriteSettingPermission(context: Context): Boolean {
            return Settings.System.canWrite(context)
        }

        fun getTetheringSettingIntent(): Intent {
            val intent = Intent()
            intent.setClassName("com.android.settings", "com.android.settings.TetherSettings")
            return intent
        }

        fun askForGpsProvider(activity: Activity) {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            activity.startActivity(intent)
        }

        fun askForDisableWifi(activity: Activity) {
            activity.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
    }

    companion object {
        private var apManager: APManager? = null

        fun getApManager(context: Context): APManager {
            if (apManager == null) {
                apManager = APManager(context)
            }
            return apManager!!
        }

        const val ERROR_GPS_PROVIDER_DISABLED = 0
        const val ERROR_LOCATION_PERMISSION_DENIED = 4
        const val ERROR_DISABLE_HOTSPOT = 1
        const val ERROR_DISABLE_WIFI = 5
        const val ERROR_WRITE_SETTINGS_PERMISSION_REQUIRED = 6
        const val ERROR_UNKNOWN = 3
        private const val ERROR_TETHERING_DISALLOWED = 2
    }
}
