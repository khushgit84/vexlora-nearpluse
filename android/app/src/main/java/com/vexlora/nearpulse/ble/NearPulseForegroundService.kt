package com.vexlora.nearpulse.ble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vexlora.nearpulse.R

class NearPulseForegroundService : Service {

    companion object {
        const val CHANNEL_ID = "nearpulse_safety_service"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_SAFETY_MONITOR"
        const val ACTION_STOP = "ACTION_STOP_SAFETY_MONITOR"
    }

    constructor() : super()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification("NearPulse Safety Shield Active", "Monitoring nearby BLE emergency beacons...")
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NearPulse Offline Safety Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps peer BLE discovery active in foreground"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
