package com.lorenzocalifano.shieldup.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.lorenzocalifano.shieldup.R
import com.lorenzocalifano.shieldup.data.FirebaseRepository
import com.lorenzocalifano.shieldup.utils.SessionManager

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val repository = FirebaseRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sessionManager = SessionManager(requireContext())

        if (sessionManager.isLogged()) {
            findNavController().navigate(R.id.homeFragment)
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

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Inserisci email e password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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

                    findNavController().navigate(R.id.homeFragment)
                },
                onError = { exception ->
                    Toast.makeText(
                        requireContext(),
                        exception.message ?: "Errore login",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
}