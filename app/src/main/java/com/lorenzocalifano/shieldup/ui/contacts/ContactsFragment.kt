package com.lorenzocalifano.shieldup.ui.contacts

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.lorenzocalifano.shieldup.R

class ContactsFragment : Fragment(R.layout.fragment_contacts) {

    private val contacts = mutableListOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backButton = view.findViewById<TextView>(R.id.btnBack)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etPhone = view.findViewById<EditText>(R.id.etPhone)
        val txtContacts = view.findViewById<TextView>(R.id.txtContacts)
        val saveButton = view.findViewById<Button>(R.id.btnSaveContact)

        loadContacts()
        updateContactsText(txtContacts)

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

            contacts.add("$name - $phone")
            saveContacts()
            updateContactsText(txtContacts)

            etName.text.clear()
            etPhone.text.clear()

            Toast.makeText(requireContext(), "Contatto salvato", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadContacts() {
        val sharedPref = requireContext().getSharedPreferences("shield_contacts", Context.MODE_PRIVATE)
        val savedText = sharedPref.getString("contacts", "") ?: ""

        contacts.clear()

        if (savedText.isNotBlank()) {
            contacts.addAll(savedText.split(";;").filter { it.isNotBlank() })
        }
    }

    private fun saveContacts() {
        val sharedPref = requireContext().getSharedPreferences("shield_contacts", Context.MODE_PRIVATE)
        sharedPref.edit()
            .putString("contacts", contacts.joinToString(";;"))
            .apply()
    }

    private fun updateContactsText(textView: TextView) {
        textView.text = if (contacts.isEmpty()) {
            "Nessun contatto salvato"
        } else {
            contacts.joinToString("\n\n")
        }
    }
}