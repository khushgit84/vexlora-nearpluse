package com.vexlora.nearpulse

import android.app.Application
import com.vexlora.nearpulse.ble.BleManager
import com.vexlora.nearpulse.data.local.NearPulseDatabase

class NearPulseApplication : Application() {
    lateinit var database: NearPulseDatabase
        private set

    lateinit var bleManager: BleManager
        private set

    override fun onCreate() {
        super.onCreate()
        com.vexlora.nearpulse.ble.EmergencyAlertManager.initialize(this)
        database = NearPulseDatabase.getDatabase(this)
        bleManager = BleManager(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        bleManager.cleanUp()
    }
}
