package com.vexlora.nearpulse.ble

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vexlora.nearpulse.MainActivity
import com.vexlora.nearpulse.R

object EmergencyAlertManager {
    private const val TAG = "EmergencyAlertManager"
    const val CHANNEL_ID_SOS = "nearpulse_emergency_sos_high_priority"
    const val CHANNEL_ID_MESSAGES = "nearpulse_chat_messages_v3"
    private const val SOS_NOTIFICATION_ID = 9999

    // Morse code for S-O-S: ... --- ...
    private val SOS_VIBRATION_PATTERN = longArrayOf(
        0,
        250, 120, 250, 120, 250, // S (3 dots)
        350,
        600, 150, 600, 150, 600, // O (3 dashes)
        350,
        250, 120, 250, 120, 250  // S (3 dots)
    )

    private val CHAT_VIBRATION_PATTERN = longArrayOf(0, 180, 80, 180)

    fun initialize(context: Context) {
        createNotificationChannels(context)
    }

    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. SOS Emergency Channel
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val alarmAudioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val sosChannel = NotificationChannel(
                CHANNEL_ID_SOS,
                "Emergency SOS Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent high-gain alarms for incoming and active SOS distress signals"
                enableVibration(true)
                vibrationPattern = SOS_VIBRATION_PATTERN
                setSound(alarmSound, alarmAudioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                enableLights(true)
                lightColor = android.graphics.Color.RED
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(sosChannel)

            // 2. Direct Chat Messages Channel (High Importance for heads-up banner outside the app!)
            val msgSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val msgAudioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build()

            val msgChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                "Direct Offline Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming peer-to-peer text messages over Bluetooth Low Energy"
                enableVibration(true)
                vibrationPattern = CHAT_VIBRATION_PATTERN
                setSound(msgSound, msgAudioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                enableLights(true)
                lightColor = android.graphics.Color.CYAN
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(msgChannel)
        }
    }

    fun postIncomingMessageNotification(context: Context, msg: BleMessage) {
        Log.d(TAG, "Posting incoming message notification from ${msg.senderPseudonym}: ${msg.text}")
        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_ROUTE", "chat")
            putExtra("EXTRA_PEER_ADDRESS", msg.peerAddress)
            putExtra("EXTRA_PEER_PSEUDONYM", msg.senderPseudonym)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (msg.peerAddress.hashCode() and 0x7FFFFFFF),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("💬 Message from ${msg.senderPseudonym}")
            .setContentText(msg.text)
            .setSubText("VEXLORA Direct BLE")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("💬 Message from ${msg.senderPseudonym}")
                    .bigText(msg.text)
                    .setSummaryText("Offline BLE Mesh")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(CHAT_VIBRATION_PATTERN)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFF00E5FF.toInt()) // Electric Cyan
            .addAction(
                android.R.drawable.ic_menu_send,
                "OPEN CHAT",
                pendingIntent
            )
            .build()

        try {
            val notifId = (msg.peerAddress.hashCode() and 0x7FFFFFFF)
            notificationManager.notify(notifId, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notification: ${e.message}", e)
        }

        // Haptic feedback vibration
        vibrateChat(context)
    }

    private fun vibrateChat(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(CHAT_VIBRATION_PATTERN, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(CHAT_VIBRATION_PATTERN, -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Chat vibration failed: ${e.message}")
        }
    }

    fun triggerSosAlarm(context: Context, alert: SosAlert) {
        Log.w(TAG, "Triggering emergency alarm for SOS from ${alert.senderPseudonym}")

        // 1. Hardware Vibration
        vibrateSos(context)

        // 2. Play Alarm Sound
        playAlarmAudio(context)

        // 3. Post Urgent High-Priority Heads-Up Notification
        postEmergencyNotification(context, alert)
    }

    fun triggerLocalSosBroadcastFeedback(context: Context) {
        // Quick sharp buzz confirming broadcast start
        vibrateSos(context)
    }

    private fun vibrateSos(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val amplitudes = intArrayOf(
                        0,
                        255, 0, 255, 0, 255,
                        0,
                        255, 0, 255, 0, 255,
                        0,
                        255, 0, 255, 0, 255
                    )
                    vibrator.vibrate(VibrationEffect.createWaveform(SOS_VIBRATION_PATTERN, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(SOS_VIBRATION_PATTERN, -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed: ${e.message}")
        }
    }

    private var activeRingtone: android.media.Ringtone? = null
    private var activeToneGenerator: ToneGenerator? = null

    private fun playAlarmAudio(context: Context) {
        stopAlarmAudio()
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, alarmUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = false
            }
            activeRingtone = ringtone
            ringtone.play()
        } catch (e: Exception) {
            Log.w(TAG, "Ringtone playback error: ${e.message}, falling back to ToneGenerator")
            try {
                val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                activeToneGenerator = tone
                tone.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1500)
            } catch (_: Exception) {}
        }
    }

    fun stopAlarmAudio() {
        try {
            activeRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
            activeRingtone = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ringtone: ${e.message}")
        }

        try {
            activeToneGenerator?.let {
                it.stopTone()
                it.release()
            }
            activeToneGenerator = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping tone generator: ${e.message}")
        }
    }

    fun stopVibration(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling vibration: ${e.message}")
        }
    }

    private fun postEmergencyNotification(context: Context, alert: SosAlert) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_ROUTE", "sos")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SOS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🚨 EMERGENCY SOS DETECTED!")
            .setContentText("${alert.senderPseudonym} is in distress (${alert.rssi} dBm)")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "⚠️ OFFLINE BLE DISTRESS BEACON ACTIVE\n" +
                    "Caller: ${alert.senderPseudonym}\n" +
                    "Signal Proximity: ${alert.rssi} dBm\n" +
                    "Details: ${alert.message}\n\n" +
                    "Tap to inspect location on radar and transmit mutual rescue response."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(SOS_VIBRATION_PATTERN)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true) // Heads-up banner pops over any active app!
            .setColor(0xFFFF1744.toInt()) // Crimson Red
            .addAction(
                android.R.drawable.ic_dialog_alert,
                "OPEN RESCUE RADAR",
                pendingIntent
            )
            .build()

        notificationManager.notify(SOS_NOTIFICATION_ID, notification)
    }

    fun dismissSosNotification(context: Context) {
        stopAlarmAudio()
        stopVibration(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(SOS_NOTIFICATION_ID)
    }
}
