package com.lorenzocalifano.shieldup.ui.contacts

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.lorenzocalifano.shieldup.R
import com.lorenzocalifano.shieldup.data.EmergencyContactDto
import com.lorenzocalifano.shieldup.data.FirebaseRepository

class ContactsFragment : Fragment(R.layout.fragment_contacts) {

    private val repository = FirebaseRepository()
    private val currentUserId = "demo_user"
    private val contacts = mutableListOf<EmergencyContactDto>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val backButton = view.findViewById<TextView>(R.id.btnBack)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etPhone = view.findViewById<EditText>(R.id.etPhone)
        val saveButton = view.findViewById<Button>(R.id.btnSaveContact)
        val contactsContainer = view.findViewById<LinearLayout>(R.id.contactsContainer)

        backButton.setOnClickListener {
            findNavController().popBackStack(R.id.homeFragment, false)
        }

        loadContactsFromFirebase(contactsContainer)

        saveButton.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "Compila tutti i campi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            repository.saveEmergencyContact(
                userId = currentUserId,
                name = name,
                phone = phone,
                onSuccess = {
                    etName.text.clear()
                    etPhone.text.clear()
                    Toast.makeText(requireContext(), "Contatto salvato", Toast.LENGTH_SHORT).show()
                    loadContactsFromFirebase(contactsContainer)
                },
                onError = {
                    Toast.makeText(requireContext(), "Errore salvataggio contatto", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun loadContactsFromFirebase(container: LinearLayout) {
        repository.loadEmergencyContacts(
            userId = currentUserId,
            onSuccess = { result ->
                contacts.clear()
                contacts.addAll(result)
                saveContactsCacheForSms()
                refreshContacts(container)
            },
            onError = {
                Toast.makeText(requireContext(), "Errore caricamento contatti", Toast.LENGTH_SHORT).show()
                refreshContacts(container)
            }
        )
    }

    private fun refreshContacts(container: LinearLayout) {
        container.removeAllViews()

        if (contacts.isEmpty()) {
            val emptyText = TextView(requireContext())
            emptyText.text = "Nessun contatto salvato"
            emptyText.textSize = 16f
            emptyText.setPadding(0, 20, 0, 0)
            container.addView(emptyText)
            return
        }

        contacts.forEach { contact ->
            container.addView(createContactCard(contact, container))
        }
    }

    private fun createContactCard(
        contact: EmergencyContactDto,
        container: LinearLayout
    ): LinearLayout {
        val card = LinearLayout(requireContext())
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.setBackgroundResource(R.drawable.bg_gray_button)
        card.setPadding(22, 18, 18, 18)

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cardParams.setMargins(0, 14, 0, 0)
        card.layoutParams = cardParams

        val textColumn = LinearLayout(requireContext())
        textColumn.orientation = LinearLayout.VERTICAL
        textColumn.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val nameText = TextView(requireContext())
        nameText.text = contact.name
        nameText.textSize = 18f
        nameText.setTextColor(resources.getColor(R.color.black, null))
        nameText.setTypeface(null, Typeface.BOLD)

        val phoneText = TextView(requireContext())
        phoneText.text = contact.phone
        phoneText.textSize = 15f
        phoneText.setTextColor(resources.getColor(R.color.black, null))
        phoneText.setPadding(0, 8, 0, 0)

        textColumn.addView(nameText)
        textColumn.addView(phoneText)

        val deleteText = TextView(requireContext())
        deleteText.text = "Elimina"
        deleteText.textSize = 13f
        deleteText.gravity = Gravity.CENTER
        deleteText.setTextColor(resources.getColor(R.color.emergency_red, null))
        deleteText.setTypeface(null, Typeface.BOLD)
        deleteText.setPadding(20, 10, 0, 10)

        deleteText.setOnClickListener {
            repository.deleteEmergencyContact(
                userId = currentUserId,
                contactId = contact.id,
                onSuccess = {
                    Toast.makeText(requireContext(), "Contatto eliminato", Toast.LENGTH_SHORT).show()
                    loadContactsFromFirebase(container)
                },
                onError = {
                    Toast.makeText(requireContext(), "Errore eliminazione contatto", Toast.LENGTH_SHORT).show()
                }
            )
        }

        card.addView(textColumn)
        card.addView(deleteText)

        return card
    }

    private fun saveContactsCacheForSms() {
        val text = contacts.joinToString(";;") {
            "${it.name} - ${it.phone}"
        }

        requireContext()
            .getSharedPreferences("shield_contacts", Context.MODE_PRIVATE)
            .edit()
            .putString("contacts", text)
            .apply()
    }
}