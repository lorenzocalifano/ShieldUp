package com.lorenzocalifano.shieldup.ui.fakecall

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lorenzocalifano.shieldup.R

object FakeCallNotification {

    const val NOTIFICATION_ID = 9901
    const val CHANNEL_ID = "fake_call_v4"

    fun showIncomingCallNotification(context: Context, callerName: String) {
        createChannel(context)

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("FAKE_CALL", "POST_NOTIFICATIONS non concesso")
            return
        }

        val fullScreenIntent = Intent(context, FakeCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("callerName", callerName)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            100,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_support)
            .setContentTitle(callerName)
            .setContentText("Chiamata in arrivo")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()

        try {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            manager.notify(NOTIFICATION_ID, notification)

            Log.d(
                "FAKE_CALL",
                "Active notifications: ${manager.activeNotifications.size}"
            )
            Toast.makeText(context, "Notifica inviata", Toast.LENGTH_LONG).show()
            Log.d("FAKE_CALL", "Notifica chiamata simulata inviata")
        } catch (exception: Exception) {
            Log.e("FAKE_CALL", "Errore invio notifica", exception)
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val ringtoneUri = Settings.System.DEFAULT_RINGTONE_URI

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Fake Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Chiamate simulate ShieldUp"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            setSound(
                ringtoneUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        Log.d(
            "FAKE_CALL",
            "Channel creato: ${
                manager.getNotificationChannel(CHANNEL_ID)?.importance
            }"
        )
    }

}