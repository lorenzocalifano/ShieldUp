package com.lorenzocalifano.shieldup.ui.fakecall

import android.app.NotificationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lorenzocalifano.shieldup.R

class FakeCallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_fake_call)

        val callerName = intent.getStringExtra("callerName") ?: "Contatto fidato"
        val alreadyAccepted = intent.getBooleanExtra("alreadyAccepted", false)

        val statusText = findViewById<TextView>(R.id.txtFakeCallStatus)
        val callerText = findViewById<TextView>(R.id.txtFakeCallerName)
        val acceptButton = findViewById<Button>(R.id.btnAcceptFakeCall)
        val rejectButton = findViewById<Button>(R.id.btnRejectFakeCall)

        callerText.text = callerName

        if (alreadyAccepted) {
            statusText.text = "Chiamata in corso"
            acceptButton.text = "Vivavoce"
            rejectButton.text = "Termina chiamata"
        }

        acceptButton.setOnClickListener {
            cancelNotification()

            statusText.text = "Chiamata in corso"
            acceptButton.text = "Vivavoce"
            rejectButton.text = "Termina chiamata"
        }

        rejectButton.setOnClickListener {
            cancelNotification()
            finish()
        }
    }

    private fun cancelNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(FakeCallNotification.NOTIFICATION_ID)
    }
}