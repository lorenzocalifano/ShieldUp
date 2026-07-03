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
        val normalizedEmail = email.trim().lowercase()
        val cleanPassword = password.trim()

        db.collection("users")
            .whereEqualTo("email", normalizedEmail)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    onError(Exception("Questa email è già registrata"))
                    return@addOnSuccessListener
                }

                val user = hashMapOf(
                    "name" to name.trim(),
                    "surname" to surname.trim(),
                    "email" to normalizedEmail,
                    "password" to cleanPassword,
                    "role" to role,
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("users")
                    .add(user)
                    .addOnSuccessListener { doc ->
                        onSuccess(
                            UserDto(
                                id = doc.id,
                                name = name.trim(),
                                surname = surname.trim(),
                                email = normalizedEmail,
                                role = role
                            )
                        )
                    }
                    .addOnFailureListener {
                        onError(Exception("Errore durante la registrazione"))
                    }
            }
            .addOnFailureListener {
                onError(Exception("Errore di connessione a Firebase"))
            }
    }

    fun loginUser(
        email: String,
        password: String,
        onSuccess: (UserDto) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalizedEmail = email.trim().lowercase()
        val cleanPassword = password.trim()

        db.collection("users")
            .whereEqualTo("email", normalizedEmail)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    onError(Exception("Utente non trovato"))
                    return@addOnSuccessListener
                }

                val doc = result.documents.first()
                val savedPassword = doc.getString("password") ?: ""

                if (savedPassword != cleanPassword) {
                    onError(Exception("Password non corretta"))
                    return@addOnSuccessListener
                }

                onSuccess(
                    UserDto(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        surname = doc.getString("surname") ?: "",
                        email = doc.getString("email") ?: normalizedEmail,
                        role = doc.getString("role") ?: "STANDARD"
                    )
                )
            }
            .addOnFailureListener {
                onError(Exception("Errore di connessione a Firebase"))
            }
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

    fun setPsychologistImmediateAvailability(
        psychologistId: String,
        psychologistName: String,
        psychologistEmail: String,
        available: Boolean,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val data = mapOf(
            "psychologistId" to psychologistId,
            "name" to psychologistName,
            "email" to psychologistEmail,
            "availableNow" to available,
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("psychologistStatus")
            .document(psychologistId)
            .set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun loadPsychologists(
        onSuccess: (List<UserDto>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("users")
            .whereEqualTo("role", "PSYCHOLOGIST")
            .get()
            .addOnSuccessListener { result ->
                val psychologists = result.documents.mapNotNull { doc ->
                    UserDto(
                        id = doc.id,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        surname = doc.getString("surname") ?: "",
                        email = doc.getString("email") ?: "",
                        role = doc.getString("role") ?: "PSYCHOLOGIST"
                    )
                }
                onSuccess(psychologists)
            }
            .addOnFailureListener { onError(it) }
    }

    fun savePsychologistAvailability(
        availability: AvailabilityDto,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val data = hashMapOf(
            "psychologistId" to availability.psychologistId,
            "psychologistName" to availability.psychologistName,
            "date" to availability.date,
            "time" to availability.time,
            "type" to availability.type,
            "booked" to availability.booked,
            "createdAt" to availability.createdAt
        )

        db.collection("availabilities")
            .add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun loadAvailableSlots(
        onSuccess: (List<AvailabilityDto>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("availabilities")
            .whereEqualTo("booked", false)
            .get()
            .addOnSuccessListener { result ->
                val slots = result.documents.mapNotNull { doc ->
                    AvailabilityDto(
                        id = doc.id,
                        psychologistId = doc.getString("psychologistId") ?: return@mapNotNull null,
                        psychologistName = doc.getString("psychologistName") ?: "",
                        date = doc.getString("date") ?: "",
                        time = doc.getString("time") ?: "",
                        type = doc.getString("type") ?: "Chat",
                        booked = doc.getBoolean("booked") ?: false,
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }.sortedByDescending { it.createdAt }

                onSuccess(slots)
            }
            .addOnFailureListener { onError(it) }
    }

    fun bookAvailability(
        availabilityId: String,
        userId: String,
        userName: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("availabilities")
            .document(availabilityId)
            .update(
                mapOf(
                    "booked" to true,
                    "bookedByUserId" to userId,
                    "bookedByUserName" to userName,
                    "bookedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun createUrgentPsychologicalRequest(
        userId: String,
        userName: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val data = hashMapOf(
            "userId" to userId,
            "userName" to userName,
            "status" to "WAITING",
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("urgentPsychologicalRequests")
            .add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun loadPendingUrgentRequests(
        onSuccess: (List<UrgentRequestDto>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("urgentPsychologicalRequests")
            .whereEqualTo("status", "WAITING")
            .get()
            .addOnSuccessListener { result ->
                val requests = result.documents.mapNotNull { doc ->
                    UrgentRequestDto(
                        id = doc.id,
                        userId = doc.getString("userId") ?: return@mapNotNull null,
                        userName = doc.getString("userName") ?: "Utente",
                        status = doc.getString("status") ?: "WAITING",
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }.sortedByDescending { it.createdAt }

                onSuccess(requests)
            }
            .addOnFailureListener { onError(it) }
    }

    fun acceptUrgentRequest(
        requestId: String,
        psychologistId: String,
        psychologistName: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("urgentPsychologicalRequests")
            .document(requestId)
            .update(
                mapOf(
                    "status" to "ACCEPTED",
                    "psychologistId" to psychologistId,
                    "psychologistName" to psychologistName,
                    "acceptedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener { onSuccess() }
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

data class AvailabilityDto(
    val id: String = "",
    val psychologistId: String,
    val psychologistName: String,
    val date: String,
    val time: String,
    val type: String,
    val booked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class UrgentRequestDto(
    val id: String,
    val userId: String,
    val userName: String,
    val status: String,
    val createdAt: Long
)