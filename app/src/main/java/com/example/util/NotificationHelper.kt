package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.app.AlarmManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_ID = "microfinance_pro_channel"
    const val CHANNEL_NAME = "Alertes MALISHO NA AKIBA (MALAKI)"
    const val CHANNEL_DESCRIPTION = "Notifications de recouvrement, remboursements, décaissements et prêts"

    /**
     * Ouvre directement les paramètres système du téléphone pour autoriser les notifications push.
     */
    fun openNotificationSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    /**
     * Vérifie si l'application dispose de l'autorisation de poster des notifications push.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return notificationManager?.areNotificationsEnabled() ?: true
    }

    fun init(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableLights(true)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 200, 300)
                    if (soundUri != null) {
                        setSound(soundUri, audioAttributes)
                    }
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.createNotificationChannel(channel)
            }
        } catch (_: Throwable) {
            // Gracefully ignore in restricted environments
        }
    }

    /**
     * Vibrate the device directly and play notification sound.
     */
    fun vibrateAndPlaySound(context: Context) {
        try {
            // 1. Physical Vibration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.let {
                    val effect = VibrationEffect.createWaveform(longArrayOf(0, 250, 100, 250), -1)
                    it.vibrate(effect)
                }
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val effect = VibrationEffect.createWaveform(longArrayOf(0, 250, 100, 250), -1)
                        it.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(longArrayOf(0, 250, 100, 250), -1)
                    }
                }
            }

            // 2. Audible notification sound
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (ringtoneUri != null) {
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, ringtoneUri)
                ringtone?.play()
            }
        } catch (_: Throwable) {
            // Ignore if silent mode or restricted in environment
        }
    }

    /**
     * Post a full system notification with sound and vibration.
     */
    fun postSystemNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt(),
        targetRoute: String = "notifications",
        clientId: String? = null
    ) {
        try {
            // Trigger direct sound and vibration safely
            vibrateAndPlaySound(context)

            // Ensure channel exists
            init(context)

            // Check POST_NOTIFICATIONS permission on Android 13+ (API 33+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("target_route", targetRoute)
                if (!clientId.isNullOrEmpty()) {
                    putExtra("client_id", clientId)
                }
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val briefOverview = if (message.length > 80) {
                message.take(77).trimEnd() + "..."
            } else {
                message
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(briefOverview)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message).setSummaryText("Toucher pour lire le détail complet"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            if (soundUri != null) {
                builder.setSound(soundUri)
            }
            builder.setVibrate(longArrayOf(0, 300, 200, 300))

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(notificationId, builder.build())
        } catch (_: Throwable) {
            // Graceful fallback
        }
    }
}
