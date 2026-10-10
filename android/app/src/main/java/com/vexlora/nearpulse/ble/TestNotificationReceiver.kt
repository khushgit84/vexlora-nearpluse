package com.vexlora.nearpulse.ble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TestNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("TestNotificationReceiver", "Broadcast received: $action")

        when (action) {
            "com.vexlora.nearpulse.TEST_NOTIFICATION" -> {
                val sender = intent.getStringExtra("sender") ?: "NP-BRAVO"
                val text = intent.getStringExtra("msg") ?: "Offline BLE mesh message: Direct radio packet received outside app."
                val address = intent.getStringExtra("address") ?: "22:33:44:55:66:77"

                val testMsg = BleMessage(
                    id = "TEST-" + System.currentTimeMillis().toString().takeLast(4),
                    peerAddress = address,
                    senderPseudonym = sender,
                    text = text,
                    timestamp = System.currentTimeMillis(),
                    isOutgoing = false,
                    isDelivered = true
                )

                EmergencyAlertManager.postIncomingMessageNotification(context, testMsg)
            }
            "com.vexlora.nearpulse.TEST_SOS" -> {
                val sender = intent.getStringExtra("sender") ?: "SOS-RESCUE"
                val text = intent.getStringExtra("msg") ?: "DISTRESS SIGNAL DETECTED NEARBY! RSSI: -42 dBm"
                val address = intent.getStringExtra("address") ?: "99:88:77:66:55:44"

                val alert = SosAlert(
                    id = "SOS-" + System.currentTimeMillis().toString().takeLast(4),
                    senderAddress = address,
                    senderPseudonym = sender,
                    message = text,
                    timestamp = System.currentTimeMillis(),
                    rssi = -42
                )

                EmergencyAlertManager.triggerSosAlarm(context, alert)
            }
        }
    }
}
