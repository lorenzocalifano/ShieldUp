package com.lorenzocalifano.shieldup.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.lorenzocalifano.shieldup.R
import com.lorenzocalifano.shieldup.data.FirebaseRepository
import com.lorenzocalifano.shieldup.utils.SessionManager

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val repository = FirebaseRepository()
    private lateinit var sessionManager: SessionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        refreshProfile(view)

        val personalDataButton = view.findViewById<Button>(R.id.btnPersonalData)
        val emergencyContactsButton = view.findViewById<Button>(R.id.btnEmergencyContacts)
        val logoutButton = view.findViewById<Button>(R.id.btnLogout)

        if (sessionManager.getRole() == "PSYCHOLOGIST") {
            emergencyContactsButton.visibility = View.GONE
        }

        personalDataButton.setOnClickListener {
            showEditProfileDialog(view)
        }

        emergencyContactsButton.setOnClickListener {
            findNavController().navigate(R.id.contactsFragment)
        }

        logoutButton.setOnClickListener {
            sessionManager.clearSession()

            findNavController().navigate(
                R.id.loginFragment,
                null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build()
            )
        }
    }

    private fun refreshProfile(view: View) {
        view.findViewById<TextView>(R.id.txtProfileName).text = sessionManager.getName()
        view.findViewById<TextView>(R.id.txtProfileEmail).text = sessionManager.getEmail()
    }

    private fun showEditProfileDialog(rootView: View) {
        val fullNameParts = sessionManager.getName().trim().split(" ")
        val currentName = fullNameParts.firstOrNull().orEmpty()
        val currentSurname = fullNameParts.drop(1).joinToString(" ")
        val currentEmail = sessionManager.getEmail()

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 0)
        }

        val nameInput = EditText(requireContext()).apply {
            hint = "Nome"
            setText(currentName)
        }

        val surnameInput = EditText(requireContext()).apply {
            hint = "Cognome"
            setText(currentSurname)
        }

        val emailInput = EditText(requireContext()).apply {
            hint = "Email"
            setText(currentEmail)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        container.addView(nameInput)
        container.addView(surnameInput)
        container.addView(emailInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Modifica dati personali")
            .setView(container)
            .setNegativeButton("Annulla", null)
            .setPositiveButton("Salva", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val name = nameInput.text.toString().trim()
                        val surname = surnameInput.text.toString().trim()
                        val email = emailInput.text.toString().trim()

                        if (name.isEmpty() || surname.isEmpty() || email.isEmpty()) {
                            Toast.makeText(requireContext(), "Compila tutti i campi", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        repository.updateUserProfile(
                            userId = sessionManager.getUserId(),
                            name = name,
                            surname = surname,
                            email = email,
                            onSuccess = {
                                sessionManager.saveSession(
                                    userId = sessionManager.getUserId(),
                                    name = "$name $surname",
                                    email = email,
                                    role = sessionManager.getRole()
                                )

                                refreshProfile(rootView)

                                Toast.makeText(
                                    requireContext(),
                                    "Dati aggiornati",
                                    Toast.LENGTH_SHORT
                                ).show()

                                dismiss()
                            },
                            onError = { exception ->
                                Toast.makeText(
                                    requireContext(),
                                    exception.message ?: "Errore aggiornamento dati",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                }
            }
            .show()
    }
}