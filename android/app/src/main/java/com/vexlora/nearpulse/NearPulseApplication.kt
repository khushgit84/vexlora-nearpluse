package com.vexlora.nearpulse

import android.app.Application
import com.vexlora.nearpulse.ble.BleManager
import com.vexlora.nearpulse.data.local.NearPulseDatabase
import com.vexlora.nearpulse.data.repository.NearPulseRepository

class NearPulseApplication : Application() {

    lateinit var database: NearPulseDatabase
        private set

    lateinit var bleManager: BleManager
        private set

    lateinit var repository: NearPulseRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = NearPulseDatabase.getDatabase(this)
        bleManager = BleManager(this)
        repository = NearPulseRepository(database, bleManager)
    }

    override fun onTerminate() {
        super.onTerminate()
        bleManager.cleanup()
    }
}
