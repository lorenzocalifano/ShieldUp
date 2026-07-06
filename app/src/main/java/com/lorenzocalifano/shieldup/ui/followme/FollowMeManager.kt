package com.lorenzocalifano.shieldup.ui.followme

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.location.LocationServices

class FollowMeManager(
    context: Context
) {

    private val repository = FollowMeRepository()
    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val handler = Handler(Looper.getMainLooper())

    private var running = false
    private var currentSessionId: String? = null

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!running) return

            updateCurrentLocation()

            handler.postDelayed(this, 5_000L)
        }
    }

    fun startTracking(sessionId: String) {
        currentSessionId = sessionId

        if (running) return

        running = true
        handler.post(updateRunnable)
    }

    fun stopTracking() {
        running = false
        currentSessionId = null
        handler.removeCallbacks(updateRunnable)
    }

    @SuppressLint("MissingPermission")
    private fun updateCurrentLocation() {
        val sessionId = currentSessionId ?: return

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (!running) return@addOnSuccessListener
                if (location == null) return@addOnSuccessListener

                repository.updateLocation(
                    sessionId = sessionId,
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            }
    }
}