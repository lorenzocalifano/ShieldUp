package com.lorenzocalifano.shieldup.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.lorenzocalifano.shieldup.R
import com.lorenzocalifano.shieldup.data.FirebaseRepository
import com.lorenzocalifano.shieldup.utils.SessionManager

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private val repository = FirebaseRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())

        val nameInput = view.findViewById<EditText>(R.id.etRegisterName)
        val surnameInput = view.findViewById<EditText>(R.id.etRegisterSurname)
        val emailInput = view.findViewById<EditText>(R.id.etRegisterEmail)
        val passwordInput = view.findViewById<EditText>(R.id.etRegisterPassword)
        val psychologistRadio = view.findViewById<RadioButton>(R.id.radioPsychologist)
        val registerButton = view.findViewById<Button>(R.id.btnRegister)
        val goLoginText = view.findViewById<TextView>(R.id.btnGoLogin)

        goLoginText.setOnClickListener {
            findNavController().popBackStack()
        }

        registerButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val surname = surnameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val role = if (psychologistRadio.isChecked) "PSYCHOLOGIST" else "STANDARD"

            clearErrors(nameInput, surnameInput, emailInput, passwordInput)

            if (!validateInputs(name, surname, email, password, nameInput, surnameInput, emailInput, passwordInput)) {
                return@setOnClickListener
            }

            registerButton.isEnabled = false
            registerButton.text = "Registrazione..."

            repository.registerUser(
                name = name,
                surname = surname,
                email = email,
                password = password,
                role = role,
                onSuccess = { user ->
                    sessionManager.saveSession(
                        userId = user.id,
                        name = "${user.name} ${user.surname}",
                        email = user.email,
                        role = user.role
                    )

                    Toast.makeText(requireContext(), "Account creato correttamente", Toast.LENGTH_SHORT).show()

                    val destination = if (user.role == "PSYCHOLOGIST") {
                        R.id.psychologistDashboardFragment
                    } else {
                        R.id.homeFragment
                    }

                    findNavController().navigate(
                        destination,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.loginFragment, true)
                            .build()
                    )
                },
                onError = { exception ->
                    registerButton.isEnabled = true
                    registerButton.text = "Registrati"

                    Toast.makeText(
                        requireContext(),
                        exception.message ?: "Errore registrazione",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun validateInputs(
        name: String,
        surname: String,
        email: String,
        password: String,
        nameInput: EditText,
        surnameInput: EditText,
        emailInput: EditText,
        passwordInput: EditText
    ): Boolean {
        if (name.isEmpty()) {
            nameInput.error = "Inserisci nome"
            return false
        }

        if (surname.isEmpty()) {
            surnameInput.error = "Inserisci cognome"
            return false
        }

        if (email.isEmpty()) {
            emailInput.error = "Inserisci email"
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Email non valida"
            return false
        }

        if (password.length < 8) {
            passwordInput.error = "La password deve avere almeno 8 caratteri"
            return false
        }

        if (!password.any { it.isUpperCase() }) {
            passwordInput.error = "Inserisci almeno una lettera maiuscola"
            return false
        }

        if (!password.any { it.isDigit() }) {
            passwordInput.error = "Inserisci almeno un numero"
            return false
        }

        return true
    }

    private fun clearErrors(vararg inputs: EditText) {
        inputs.forEach { it.error = null }
    }
}