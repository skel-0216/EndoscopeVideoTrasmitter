package com.example.endoscopevideotrasmitter

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.endoscopevideotrasmitter.databinding.ActivityHotspotBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix

class HotspotActivity : AppCompatActivity() {
    private val binding by lazy { ActivityHotspotBinding.inflate(layoutInflater) }

    private val TAG = "HotspotActivity"

    private lateinit var myApp : GlobalUsages

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d(TAG, "${it.key} = ${it.value}")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (it.key == Manifest.permission.NEARBY_WIFI_DEVICES && !it.value) {
                        Toast.makeText(this, "Please allow nearby permission", Toast.LENGTH_LONG)
                            .show()
                    } else {
                        enableHotspot()
                    }
                } else {
                    if (it.key == Manifest.permission.ACCESS_FINE_LOCATION && !it.value) {
                        Toast.makeText(this, "Please allow nearby permission", Toast.LENGTH_LONG)
                            .show()
                    } else {
                        enableHotspot()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        myApp = application as GlobalUsages

        initUI()

    }

    @SuppressLint("SetTextI18n")
    private fun initUI() {
        val isHotspotOn = myApp.apManager.isWifiApEnabled()
        Log.d("==============================","isWifiApEnabled $isHotspotOn")
        if(isHotspotOn){
            binding.btQrCode.text = "HotSpot is already running"
        }
        binding.btQrCode.setOnClickListener {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
            binding.btQrCode.text = "HotSpot is already running"
        }
    }

    private fun enableHotspot() {
        Log.d(TAG, "enableHotspot()")
        myApp.apManager.turnOnHotspot(this,
            object : APManager.OnSuccessListener {
                override fun onSuccess(ssid: String, password: String) {
                    Log.d(TAG, "ssid $ssid password : $password")
                    val qrCode = generateQRCode(ssid, password)
                    binding.imgQrCode.setImageBitmap(qrCode)
                }
            },
            object : APManager.OnFailureListener {
                override fun onFailure(failureCode: Int, e: Exception?) {
                    Log.e(TAG, "Error : ${e?.message} failureCode : $failureCode")
                }
            }
        )
    }

    @Throws(WriterException::class)
    fun generateQRCode(ssid: String, password: String): Bitmap? {
        Log.d(TAG, "generateQRCode() ssid : $ssid password : $password")
        binding.tvSSid.text = "SSID : $ssid"
        binding.tvPassword.text = "Password : $password"

        val qrCodeContent = "WIFI:S:$ssid;T:WPA;P:$password;;"
        val size = 800 // pixels
        val qrCodeCanvas: BitMatrix = MultiFormatWriter().encode(
            qrCodeContent,
            BarcodeFormat.QR_CODE,
            size,
            size
        )
        val w = qrCodeCanvas.width
        val h = qrCodeCanvas.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (qrCodeCanvas[x, y]) Color.BLACK else Color.WHITE
            }
        }
        val qrBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        qrBitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return qrBitmap
    }
}