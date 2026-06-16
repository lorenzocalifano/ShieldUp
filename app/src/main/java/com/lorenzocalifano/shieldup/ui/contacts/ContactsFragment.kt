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

class ContactsFragment : Fragment(R.layout.fragment_contacts) {

    private val contacts = mutableListOf<ContactItem>()

    data class ContactItem(
        val name: String,
        val phone: String
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val backButton = view.findViewById<TextView>(R.id.btnBack)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etPhone = view.findViewById<EditText>(R.id.etPhone)
        val saveButton = view.findViewById<Button>(R.id.btnSaveContact)
        val contactsContainer = view.findViewById<LinearLayout>(R.id.contactsContainer)

        loadContacts()
        refreshContacts(contactsContainer)

        backButton.setOnClickListener {
            findNavController().popBackStack(R.id.homeFragment, false)
        }

        saveButton.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "Compila tutti i campi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            contacts.add(ContactItem(name, phone))
            saveContacts()
            refreshContacts(contactsContainer)

            etName.text.clear()
            etPhone.text.clear()

            Toast.makeText(requireContext(), "Contatto salvato", Toast.LENGTH_SHORT).show()
        }
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

        contacts.forEachIndexed { index, contact ->
            container.addView(createContactCard(contact, index, container))
        }
    }

    private fun createContactCard(
        contact: ContactItem,
        index: Int,
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

        val textParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        textColumn.layoutParams = textParams

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

        val deleteButton = TextView(requireContext())

        deleteButton.text = "Elimina"
        deleteButton.textSize = 13f
        deleteButton.gravity = Gravity.CENTER
        deleteButton.setTextColor(resources.getColor(R.color.emergency_red, null))
        deleteButton.setTypeface(null, Typeface.BOLD)
        deleteButton.setPadding(20, 10, 20, 10)

        val deleteParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        
        deleteButton.layoutParams = deleteParams
        deleteButton.setOnClickListener {
            contacts.removeAt(index)
            saveContacts()
            refreshContacts(container)
        }

        card.addView(textColumn)
        card.addView(deleteButton)

        return card
    }

    private fun loadContacts() {
        val savedText = requireContext()
            .getSharedPreferences("shield_contacts", Context.MODE_PRIVATE)
            .getString("contacts", "") ?: ""

        contacts.clear()

        if (savedText.isBlank()) return

        savedText.split(";;").forEach { row ->
            val parts = row.split(" - ")
            if (parts.size >= 2) {
                contacts.add(ContactItem(parts[0].trim(), parts[1].trim()))
            }
        }
    }

    private fun saveContacts() {
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