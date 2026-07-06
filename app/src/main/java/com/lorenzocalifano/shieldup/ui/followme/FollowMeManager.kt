package com.lorenzocalifano.shieldup.ui.followme

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.*

class FollowMeManager(
    context: Context
) {

    private val repository = FollowMeRepository()

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startTracking(
        sessionId: String
    ) {

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        )
            .setMinUpdateIntervalMillis(5000L)
            .build()

        callback = object : LocationCallback() {

            override fun onLocationResult(result: LocationResult) {

                val location: Location = result.lastLocation ?: return

                repository.updateLocation(
                    sessionId = sessionId,
                    latitude = location.latitude,
                    longitude = location.longitude
                )

            }

        }

        fusedLocationClient.requestLocationUpdates(
            request,
            callback!!,
            null
        )

    }

    fun stopTracking() {

        callback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }

        callback = null

    }

}