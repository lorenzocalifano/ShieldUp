package com.lorenzocalifano.shieldup.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()

    fun registerUser(
        name: String,
        surname: String,
        email: String,
        password: String,
        role: String,
        onSuccess: (UserDto) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    onError(Exception("Email già registrata"))
                    return@addOnSuccessListener
                }

                val user = hashMapOf(
                    "name" to name,
                    "surname" to surname,
                    "email" to email,
                    "password" to password,
                    "role" to role,
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("users")
                    .add(user)
                    .addOnSuccessListener { doc ->
                        onSuccess(
                            UserDto(
                                id = doc.id,
                                name = name,
                                surname = surname,
                                email = email,
                                role = role
                            )
                        )
                    }
                    .addOnFailureListener { onError(it) }
            }
            .addOnFailureListener { onError(it) }
    }

    fun loginUser(
        email: String,
        password: String,
        onSuccess: (UserDto) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("users")
            .whereEqualTo("email", email)
            .whereEqualTo("password", password)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    onError(Exception("Credenziali non valide"))
                    return@addOnSuccessListener
                }

                val doc = result.documents.first()

                onSuccess(
                    UserDto(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        surname = doc.getString("surname") ?: "",
                        email = doc.getString("email") ?: "",
                        role = doc.getString("role") ?: "STANDARD"
                    )
                )
            }
            .addOnFailureListener { onError(it) }
    }

    fun saveEmergencyContact(
        userId: String,
        name: String,
        phone: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val contact = hashMapOf(
            "name" to name,
            "phone" to phone,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(userId)
            .collection("emergencyContacts")
            .add(contact)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun loadEmergencyContacts(
        userId: String,
        onSuccess: (List<EmergencyContactDto>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("users")
            .document(userId)
            .collection("emergencyContacts")
            .get()
            .addOnSuccessListener { result ->
                val contacts = result.documents.mapNotNull { doc ->
                    EmergencyContactDto(
                        id = doc.id,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        phone = doc.getString("phone") ?: return@mapNotNull null
                    )
                }
                onSuccess(contacts)
            }
            .addOnFailureListener { onError(it) }
    }

    fun deleteEmergencyContact(
        userId: String,
        contactId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("users")
            .document(userId)
            .collection("emergencyContacts")
            .document(contactId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun saveRedZone(
        redZone: RedZoneDto,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val data = hashMapOf(
            "title" to redZone.title,
            "description" to redZone.description,
            "latitude" to redZone.latitude,
            "longitude" to redZone.longitude,
            "createdAt" to redZone.createdAt,
            "userId" to redZone.userId,
            "type" to redZone.type
        )

        db.collection("redZones")
            .add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun loadRedZones(
        onSuccess: (List<RedZoneDto>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("redZones")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val zones = result.documents.mapNotNull { doc ->
                    RedZoneDto(
                        id = doc.id,
                        title = doc.getString("title") ?: return@mapNotNull null,
                        description = doc.getString("description") ?: return@mapNotNull null,
                        latitude = doc.getDouble("latitude") ?: return@mapNotNull null,
                        longitude = doc.getDouble("longitude") ?: return@mapNotNull null,
                        createdAt = doc.getLong("createdAt") ?: return@mapNotNull null,
                        userId = doc.getString("userId") ?: "unknown",
                        type = doc.getString("type") ?: "Pericolo"
                    )
                }
                onSuccess(zones)
            }
            .addOnFailureListener { onError(it) }
    }
}

data class UserDto(
    val id: String,
    val name: String,
    val surname: String,
    val email: String,
    val role: String
)

data class EmergencyContactDto(
    val id: String,
    val name: String,
    val phone: String
)

data class RedZoneDto(
    val id: String = "",
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long,
    val userId: String,
    val type: String = "Pericolo"
)