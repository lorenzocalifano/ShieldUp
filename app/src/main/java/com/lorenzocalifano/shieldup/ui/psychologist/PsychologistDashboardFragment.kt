package com.lorenzocalifano.shieldup.ui.psychologist

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

class PsychologistDashboardFragment : Fragment(R.layout.fragment_psychologist_dashboard) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sessionManager = SessionManager(requireContext())

        val infoText = view.findViewById<TextView>(R.id.txtPsychologistInfo)
        val createAvailabilityButton = view.findViewById<Button>(R.id.btnCreateAvailability)
        val toggleImmediateButton = view.findViewById<Button>(R.id.btnToggleImmediate)
        val urgentRequestsButton = view.findViewById<Button>(R.id.btnUrgentRequests)
        val logoutButton = view.findViewById<Button>(R.id.btnPsychologistLogout)

        infoText.text = "${sessionManager.getName()}\n${sessionManager.getEmail()}"

        createAvailabilityButton.setOnClickListener {
            Toast.makeText(requireContext(), "Creazione disponibilità da implementare", Toast.LENGTH_SHORT).show()
        }

        toggleImmediateButton.setOnClickListener {
            Toast.makeText(requireContext(), "Disponibilità immediata attivata", Toast.LENGTH_SHORT).show()
        }

        urgentRequestsButton.setOnClickListener {
            Toast.makeText(requireContext(), "Nessuna richiesta urgente al momento", Toast.LENGTH_SHORT).show()
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