package com.lorenzocalifano.shieldup.data

import com.google.firebase.firestore.FirebaseFirestore

class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()

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
            .addOnFailureListener { exception -> onError(exception) }
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
                    val name = doc.getString("name")
                    val phone = doc.getString("phone")

                    if (name != null && phone != null) {
                        EmergencyContactDto(
                            id = doc.id,
                            name = name,
                            phone = phone
                        )
                    } else {
                        null
                    }
                }

                onSuccess(contacts)
            }
            .addOnFailureListener { exception -> onError(exception) }
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
            .addOnFailureListener { exception -> onError(exception) }
    }
}

data class EmergencyContactDto(
    val id: String,
    val name: String,
    val phone: String
)