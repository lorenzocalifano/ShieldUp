package com.lorenzocalifano.shieldup.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.lorenzocalifano.shieldup.R
import com.lorenzocalifano.shieldup.data.FirebaseRepository
import com.lorenzocalifano.shieldup.utils.SessionManager

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val repository = FirebaseRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())

        if (sessionManager.isLogged()) {
            navigateByRole(sessionManager.getRole())
            return
        }

        val emailInput = view.findViewById<EditText>(R.id.etLoginEmail)
        val passwordInput = view.findViewById<EditText>(R.id.etLoginPassword)
        val loginButton = view.findViewById<Button>(R.id.btnLogin)
        val registerButton = view.findViewById<Button>(R.id.btnGoRegister)

        registerButton.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            emailInput.error = null
            passwordInput.error = null

            if (email.isEmpty()) {
                emailInput.error = "Inserisci email"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordInput.error = "Inserisci password"
                return@setOnClickListener
            }

            loginButton.isEnabled = false
            loginButton.text = "Accesso..."

            repository.loginUser(
                email = email,
                password = password,
                onSuccess = { user ->
                    sessionManager.saveSession(
                        userId = user.id,
                        name = "${user.name} ${user.surname}",
                        email = user.email,
                        role = user.role
                    )

                    Toast.makeText(requireContext(), "Accesso effettuato", Toast.LENGTH_SHORT).show()
                    navigateByRole(user.role)
                },
                onError = { exception ->
                    loginButton.isEnabled = true
                    loginButton.text = "Accedi"

                    Toast.makeText(
                        requireContext(),
                        exception.message ?: "Errore login",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun navigateByRole(role: String) {
        val destination = if (role == "PSYCHOLOGIST") {
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
    }
}