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
                        onSuccess(UserDto(doc.id, name.trim(), surname.trim(), normalizedEmail, role))
                    }
                    .addOnFailureListener { onError(Exception("Errore durante la registrazione")) }
            }
            .addOnFailureListener { onError(Exception("Errore di connessione a Firebase")) }
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
            .addOnFailureListener { onError(Exception("Errore di connessione a Firebase")) }
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
            "scheduledAt" to availability.scheduledAt,
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

    fun loadPsychologistAvailabilities(
        psychologistId: String,
        onSuccess: (List<AvailabilityDto>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("availabilities")
            .whereEqualTo("psychologistId", psychologistId)
            .get()
            .addOnSuccessListener { result ->
                val slots = result.documents.mapNotNull { doc ->
                    AvailabilityDto(
                        id = doc.id,
                        psychologistId = doc.getString("psychologistId") ?: return@mapNotNull null,
                        psychologistName = doc.getString("psychologistName") ?: "",
                        date = doc.getString("date") ?: "",
                        time = doc.getString("time") ?: "",
                        scheduledAt = doc.getLong("scheduledAt") ?: 0L,
                        type = doc.getString("type") ?: "Chat",
                        booked = doc.getBoolean("booked") ?: false,
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }.filter { it.scheduledAt > System.currentTimeMillis() }
                 .sortedBy { it.scheduledAt }

                onSuccess(slots)
            }
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
                        scheduledAt = doc.getLong("scheduledAt") ?: 0L,
                        type = doc.getString("type") ?: "Chat",
                        booked = doc.getBoolean("booked") ?: false,
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }.filter { it.scheduledAt > System.currentTimeMillis() }
                 .sortedBy { it.scheduledAt }

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

    fun deleteAvailability(
        availabilityId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("availabilities")
            .document(availabilityId)
            .delete()
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

    fun createChatFromUrgentRequest(
        request: UrgentRequestDto,
        psychologistId: String,
        psychologistName: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val chatData = hashMapOf(
            "userId" to request.userId,
            "userName" to request.userName,
            "psychologistId" to psychologistId,
            "psychologistName" to psychologistName,
            "lastMessage" to "Chat avviata",
            "lastMessageAt" to System.currentTimeMillis(),
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("chats")
            .add(chatData)
            .addOnSuccessListener { chatDoc ->
                db.collection("urgentPsychologicalRequests")
                    .document(request.id)
                    .update(
                        mapOf(
                            "status" to "ACCEPTED",
                            "psychologistId" to psychologistId,
                            "psychologistName" to psychologistName,
                            "chatId" to chatDoc.id,
                            "acceptedAt" to System.currentTimeMillis()
                        )
                    )
                    .addOnSuccessListener {
                        onSuccess(chatDoc.id)
                    }
                    .addOnFailureListener { onError(it) }
            }
            .addOnFailureListener { onError(it) }
    }

    fun loadChatsForUser(
        userId: String,
        role: String,
        onSuccess: (List<ChatDto>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val field = if (role == "PSYCHOLOGIST") "psychologistId" else "userId"

        db.collection("chats")
            .whereEqualTo(field, userId)
            .get()
            .addOnSuccessListener { result ->
                val chats = result.documents.mapNotNull { doc ->
                    ChatDto(
                        id = doc.id,
                        userId = doc.getString("userId") ?: return@mapNotNull null,
                        userName = doc.getString("userName") ?: "",
                        psychologistId = doc.getString("psychologistId") ?: "",
                        psychologistName = doc.getString("psychologistName") ?: "",
                        lastMessage = doc.getString("lastMessage") ?: "Nessun messaggio",
                        lastMessageAt = doc.getLong("lastMessageAt") ?: 0L,
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }.sortedByDescending { it.lastMessageAt }

                onSuccess(chats)
            }
            .addOnFailureListener { onError(it) }
    }

    fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        text: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val now = System.currentTimeMillis()

        val message = hashMapOf(
            "senderId" to senderId,
            "senderName" to senderName,
            "text" to text,
            "createdAt" to now
        )

        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                db.collection("chats")
                    .document(chatId)
                    .update(
                        mapOf(
                            "lastMessage" to text,
                            "lastMessageAt" to now
                        )
                    )
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onError(it) }
            }
            .addOnFailureListener { onError(it) }
    }

    fun loadMessages(
        chatId: String,
        onSuccess: (List<MessageDto>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                val messages = result.documents.mapNotNull { doc ->
                    MessageDto(
                        id = doc.id,
                        senderId = doc.getString("senderId") ?: return@mapNotNull null,
                        senderName = doc.getString("senderName") ?: "",
                        text = doc.getString("text") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }

                onSuccess(messages)
            }
            .addOnFailureListener { onError(it) }
    }

    fun deleteChat(
        chatId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val chatRef = db.collection("chats").document(chatId)

        chatRef.collection("messages")
            .get()
            .addOnSuccessListener { messages ->
                val batch = db.batch()

                messages.documents.forEach { message ->
                    batch.delete(message.reference)
                }

                batch.delete(chatRef)

                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onError(it) }
            }
            .addOnFailureListener { onError(it) }
    }

    fun deleteWaitingUrgentRequestsForUser(
        userId: String,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        db.collection("urgentPsychologicalRequests")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "WAITING")
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()

                result.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onError(it) }
            }
            .addOnFailureListener { onError(it) }
    }

    fun createLiveLocationSession(
        userId: String,
        userName: String,
        destination: String,
        latitude: Double,
        longitude: Double,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val now = System.currentTimeMillis()
        val expiresAt = now + (3L * 60L * 60L * 1000L)

        val data = hashMapOf(
            "userId" to userId,
            "userName" to userName,
            "destination" to destination,
            "latitude" to latitude,
            "longitude" to longitude,
            "active" to true,
            "createdAt" to now,
            "updatedAt" to now,
            "expiresAt" to expiresAt
        )

        db.collection("liveLocations")
            .add(data)
            .addOnSuccessListener { doc ->
                onSuccess(doc.id)
            }
            .addOnFailureListener { onError(it) }
    }

    fun updateLiveLocation(
        sessionId: String,
        latitude: Double,
        longitude: Double,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
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
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun stopLiveLocationSession(
        sessionId: String,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        db.collection("liveLocations")
            .document(sessionId)
            .update(
                mapOf(
                    "active" to false,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun loadLiveLocationSession(
        sessionId: String,
        onSuccess: (LiveLocationDto?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("liveLocations")
            .document(sessionId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }

                val session = LiveLocationDto(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    userName = doc.getString("userName") ?: "",
                    destination = doc.getString("destination") ?: "",
                    latitude = doc.getDouble("latitude") ?: 0.0,
                    longitude = doc.getDouble("longitude") ?: 0.0,
                    active = doc.getBoolean("active") ?: false,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    updatedAt = doc.getLong("updatedAt") ?: 0L,
                    expiresAt = doc.getLong("expiresAt") ?: 0L
                )

                onSuccess(session)
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

data class AvailabilityDto(
    val id: String = "",
    val psychologistId: String,
    val psychologistName: String,
    val date: String,
    val time: String,
    val scheduledAt: Long = 0L,
    val type: String = "Chat",
    val booked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class UrgentRequestDto(
    val id: String,
    val userId: String,
    val userName: String,
    val status: String,
    val createdAt: Long,
    val expiresAt: Long = 0L
)

data class ChatDto(
    val id: String,
    val userId: String,
    val userName: String,
    val psychologistId: String,
    val psychologistName: String,
    val lastMessage: String,
    val lastMessageAt: Long,
    val createdAt: Long
)

data class MessageDto(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val createdAt: Long
)

data class LiveLocationDto(
    val id: String = "",
    val userId: String,
    val userName: String,
    val destination: String,
    val latitude: Double,
    val longitude: Double,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val expiresAt: Long
)