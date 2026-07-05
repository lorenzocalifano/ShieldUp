package com.lorenzocalifano.shieldup.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.lorenzocalifano.shieldup.R
import com.lorenzocalifano.shieldup.utils.SessionManager

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())

        view.findViewById<TextView>(R.id.txtProfileName).text = sessionManager.getName()
        view.findViewById<TextView>(R.id.txtProfileEmail).text = sessionManager.getEmail()

        val personalDataButton = view.findViewById<Button>(R.id.btnPersonalData)
        val emergencyContactsButton = view.findViewById<Button>(R.id.btnEmergencyContacts)
        val logoutButton = view.findViewById<Button>(R.id.btnLogout)

        if (sessionManager.getRole() == "PSYCHOLOGIST") {
            emergencyContactsButton.visibility = View.GONE
        } else {
            emergencyContactsButton.visibility = View.VISIBLE
        }

        personalDataButton.setOnClickListener {
            Toast.makeText(requireContext(), "Modifica dati da implementare", Toast.LENGTH_SHORT).show()
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
}