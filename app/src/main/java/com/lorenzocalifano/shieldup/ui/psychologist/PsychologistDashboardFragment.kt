package com.lorenzocalifano.shieldup.ui.psychologist

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.lorenzocalifano.shieldup.R
import com.lorenzocalifano.shieldup.data.AvailabilityDto
import com.lorenzocalifano.shieldup.data.FirebaseRepository
import com.lorenzocalifano.shieldup.utils.SessionManager

class PsychologistDashboardFragment : Fragment(R.layout.fragment_psychologist_dashboard) {

    private val repository = FirebaseRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sessionManager = SessionManager(requireContext())

        val infoText = view.findViewById<android.widget.TextView>(R.id.txtPsychologistInfo)
        val createAvailabilityButton = view.findViewById<Button>(R.id.btnCreateAvailability)
        val toggleImmediateButton = view.findViewById<Button>(R.id.btnToggleImmediate)
        val urgentRequestsButton = view.findViewById<Button>(R.id.btnUrgentRequests)
        val logoutButton = view.findViewById<Button>(R.id.btnPsychologistLogout)

        infoText.text = "${sessionManager.getName()}\n${sessionManager.getEmail()}"

        createAvailabilityButton.setOnClickListener {
            showCreateAvailabilityDialog(sessionManager)
        }

        toggleImmediateButton.setOnClickListener {
            Toast.makeText(requireContext(), "Disponibilità immediata da collegare", Toast.LENGTH_SHORT).show()
        }

        urgentRequestsButton.setOnClickListener {
            Toast.makeText(requireContext(), "Richieste urgenti da implementare", Toast.LENGTH_SHORT).show()
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

    private fun showCreateAvailabilityDialog(sessionManager: SessionManager) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_availability, null)

        val dateInput = dialogView.findViewById<EditText>(R.id.etAvailabilityDate)
        val timeInput = dialogView.findViewById<EditText>(R.id.etAvailabilityTime)
        val radioVideo = dialogView.findViewById<RadioButton>(R.id.radioVideo)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setNegativeButton("Annulla", null)
            .setPositiveButton("Salva", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val date = dateInput.text.toString().trim()
                val time = timeInput.text.toString().trim()
                val type = if (radioVideo.isChecked) "Videochat" else "Chat"

                if (date.isEmpty() || time.isEmpty()) {
                    Toast.makeText(requireContext(), "Compila data e ora", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val availability = AvailabilityDto(
                    psychologistId = sessionManager.getUserId(),
                    psychologistName = sessionManager.getName(),
                    date = date,
                    time = time,
                    type = type
                )

                repository.savePsychologistAvailability(
                    availability = availability,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Disponibilità salvata", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    },
                    onError = { exception ->
                        Toast.makeText(
                            requireContext(),
                            exception.message ?: "Errore salvataggio disponibilità",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }

        dialog.show()
    }
}