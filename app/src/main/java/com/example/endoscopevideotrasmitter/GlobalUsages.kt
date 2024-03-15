package com.example.endoscopevideotrasmitter

import android.app.Application

class GlobalUsages : Application() {
    lateinit var apManager : APManager
    var isHotspotTurnOn = false

    override fun onCreate() {
        super.onCreate()

        apManager = APManager.getApManager(this)
    }
}
