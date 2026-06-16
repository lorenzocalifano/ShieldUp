package com.lorenzocalifano.shieldup.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.lorenzocalifano.shieldup.R

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnEmergencyContacts).setOnClickListener {
            findNavController().navigate(R.id.contactsFragment)
        }

        view.findViewById<Button>(R.id.btnPersonalData).setOnClickListener {
            Toast.makeText(requireContext(), "Modifica dati da implementare", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnAssistance).setOnClickListener {
            Toast.makeText(requireContext(), "Richiesta assistenza inviata", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            Toast.makeText(requireContext(), "Logout simulato", Toast.LENGTH_SHORT).show()
        }
    }
}