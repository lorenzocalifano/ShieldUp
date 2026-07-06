package com.lorenzocalifano.shieldup.ui.followme

import com.google.firebase.firestore.FirebaseFirestore

class FollowMeRepository {

    private val db = FirebaseFirestore.getInstance()

    fun createSession(
        session: FollowMeSessionDto,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {

        db.collection("liveLocations")
            .add(session)
            .addOnSuccessListener {

                it.update(
                    mapOf(
                        "id" to it.id
                    )
                )

                onSuccess(it.id)
            }
            .addOnFailureListener(onError)

    }

    fun updateLocation(
        sessionId: String,
        latitude: Double,
        longitude: Double
    ) {

        db.collection("liveLocations")
            .document(sessionId)
            .update(
                mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "updatedAt" to System.currentTimeMillis()
                )
            )

    }

    fun stopSession(
        sessionId: String
    ) {

        db.collection("liveLocations")
            .document(sessionId)
            .update(
                mapOf(
                    "active" to false,
                    "updatedAt" to System.currentTimeMillis()
                )
            )

    }

}