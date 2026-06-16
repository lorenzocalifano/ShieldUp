package com.lorenzocalifano.shieldup.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
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

            if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Compila tutti i campi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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

                    Toast.makeText(requireContext(), "Account creato", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.homeFragment)
                },
                onError = { exception ->
                    Toast.makeText(
                        requireContext(),
                        exception.message ?: "Errore registrazione",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
}