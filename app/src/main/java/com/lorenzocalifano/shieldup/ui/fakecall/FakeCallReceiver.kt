package com.lorenzocalifano.shieldup.ui.fakecall

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class FakeCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val callerName = intent.getStringExtra("callerName") ?: "Contatto fidato"
        val action = intent.action

        when (action) {
            ACTION_SHOW_CALL -> {
                FakeCallNotification.showIncomingCallNotification(context, callerName)
            }

            ACTION_ACCEPT_CALL -> {
                cancelNotification(context)

                val activityIntent = Intent(context, FakeCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("callerName", callerName)
                    putExtra("alreadyAccepted", true)
                }

                ContextCompat.startActivity(context, activityIntent, null)
            }

            ACTION_REJECT_CALL -> {
                cancelNotification(context)
            }
        }
    }

    private fun cancelNotification(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(FakeCallNotification.NOTIFICATION_ID)
    }

    companion object {
        const val ACTION_SHOW_CALL = "com.lorenzocalifano.shieldup.SHOW_FAKE_CALL"
        const val ACTION_ACCEPT_CALL = "com.lorenzocalifano.shieldup.ACCEPT_FAKE_CALL"
        const val ACTION_REJECT_CALL = "com.lorenzocalifano.shieldup.REJECT_FAKE_CALL"
    }
}