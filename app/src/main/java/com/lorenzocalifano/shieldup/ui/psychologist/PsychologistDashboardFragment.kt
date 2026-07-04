package com.lorenzocalifano.shieldup.ui.psychologist

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.lorenzocalifano.shieldup.R
import com.lorenzocalifano.shieldup.data.AvailabilityDto
import com.lorenzocalifano.shieldup.data.FirebaseRepository
import com.lorenzocalifano.shieldup.data.UrgentRequestDto
import com.lorenzocalifano.shieldup.utils.SessionManager
import java.util.Calendar

class PsychologistDashboardFragment : Fragment(R.layout.fragment_psychologist_dashboard) {

    private val repository = FirebaseRepository()
    private var availableNow = false

    private lateinit var sessionManager: SessionManager
    private lateinit var availabilitiesContainer: LinearLayout
    private lateinit var urgentRequestsContainer: LinearLayout
    private lateinit var toggleImmediateButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        val infoText = view.findViewById<TextView>(R.id.txtPsychologistInfo)
        val createAvailabilityButton = view.findViewById<Button>(R.id.btnCreateAvailability)
        toggleImmediateButton = view.findViewById(R.id.btnToggleImmediate)
        val logoutButton = view.findViewById<Button>(R.id.btnPsychologistLogout)

        availabilitiesContainer = view.findViewById(R.id.availabilitiesContainer)
        urgentRequestsContainer = view.findViewById(R.id.urgentRequestsContainer)

        infoText.text = "${sessionManager.getName()}\n${sessionManager.getEmail()}"

        createAvailabilityButton.setOnClickListener {
            showCreateAvailabilityDialog()
        }

        toggleImmediateButton.setOnClickListener {
            toggleImmediateAvailability()
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

        refreshDashboard()
    }

    private fun showCreateAvailabilityDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_availability, null)

        val pickDateButton = dialogView.findViewById<Button>(R.id.btnPickDate)
        val pickTimeButton = dialogView.findViewById<Button>(R.id.btnPickTime)
        val selectedDateText = dialogView.findViewById<TextView>(R.id.txtSelectedDate)
        val selectedTimeText = dialogView.findViewById<TextView>(R.id.txtSelectedTime)
        val radioVideo = dialogView.findViewById<RadioButton>(R.id.radioVideo)

        var selectedDate = ""
        var selectedTime = ""

        pickDateButton.setOnClickListener {
            val calendar = Calendar.getInstance()

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedDate = "%02d/%02d/%04d".format(dayOfMonth, month + 1, year)
                    selectedDateText.text = selectedDate
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        pickTimeButton.setOnClickListener {
            val calendar = Calendar.getInstance()

            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    selectedTime = "%02d:%02d".format(hourOfDay, minute)
                    selectedTimeText.text = selectedTime
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setNegativeButton("Annulla", null)
            .setPositiveButton("Salva", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (selectedDate.isEmpty()) {
                    Toast.makeText(requireContext(), "Seleziona una data", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (selectedTime.isEmpty()) {
                    Toast.makeText(requireContext(), "Seleziona un orario", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val availability = AvailabilityDto(
                    psychologistId = sessionManager.getUserId(),
                    psychologistName = sessionManager.getName(),
                    date = selectedDate,
                    time = selectedTime,
                    type = if (radioVideo.isChecked) "Videochat" else "Chat"
                )

                repository.savePsychologistAvailability(
                    availability = availability,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Disponibilità salvata", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadAvailabilities()
                    },
                    onError = {
                        Toast.makeText(requireContext(), "Errore salvataggio disponibilità", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }

        dialog.show()
    }

    private fun toggleImmediateAvailability() {
        availableNow = !availableNow

        repository.setPsychologistImmediateAvailability(
            psychologistId = sessionManager.getUserId(),
            psychologistName = sessionManager.getName(),
            psychologistEmail = sessionManager.getEmail(),
            available = availableNow,
            onSuccess = {
                updateImmediateButton()
                Toast.makeText(
                    requireContext(),
                    if (availableNow) "Ora sei disponibile per richieste immediate" else "Disponibilità immediata disattivata",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onError = {
                availableNow = !availableNow
                Toast.makeText(requireContext(), "Errore aggiornamento disponibilità", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun updateImmediateButton() {
        toggleImmediateButton.text = if (availableNow) {
            "Disponibile ora: ATTIVO"
        } else {
            "Sono disponibile ora"
        }
    }

    private fun refreshDashboard() {
        updateImmediateButton()
        loadAvailabilities()
        loadUrgentRequests()
    }

    private fun loadAvailabilities() {
        repository.loadPsychologistAvailabilities(
            psychologistId = sessionManager.getUserId(),
            onSuccess = { availabilities ->
                if (!isAdded || context == null) return@loadPsychologistAvailabilities
                availabilitiesContainer.removeAllViews()

                if (availabilities.isEmpty()) {
                    availabilitiesContainer.addView(createEmptyText("Nessuna disponibilità creata"))
                    return@loadPsychologistAvailabilities
                }

                availabilities.forEach { availability ->
                    availabilitiesContainer.addView(createAvailabilityCard(availability))
                }
            },
            onError = {
                if (!isAdded || context == null) return@loadPsychologistAvailabilities
                Toast.makeText(requireContext(), "Errore caricamento disponibilità", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun loadUrgentRequests() {
        repository.loadPendingUrgentRequests(
            onSuccess = { requests ->
                if (!isAdded || context == null) return@loadPendingUrgentRequests

                urgentRequestsContainer.removeAllViews()

                if (requests.isEmpty()) {
                    urgentRequestsContainer.addView(createEmptyText("Nessuna richiesta urgente in attesa"))
                    return@loadPendingUrgentRequests
                }

                requests.forEach { request ->
                    if (!isAdded || context == null) return@loadPendingUrgentRequests
                    urgentRequestsContainer.addView(createUrgentRequestCard(request))
                }
            },
            onError = {
                if (!isAdded || context == null) return@loadPendingUrgentRequests

                Toast.makeText(
                    requireContext(),
                    "Errore caricamento richieste urgenti",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun createAvailabilityCard(availability: AvailabilityDto): View {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 18, 22, 18)
            background = ContextCompatCompat.getDrawableSafe(this@PsychologistDashboardFragment, R.drawable.bg_gray_button)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 14)
            layoutParams = params
        }

        val title = TextView(requireContext()).apply {
            text = "${availability.date} - ${availability.time}"
            textSize = 18f
            setTextColor(resources.getColor(R.color.black, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val subtitle = TextView(requireContext()).apply {
            text = "${availability.type} • ${if (availability.booked) "Prenotata" else "Disponibile"}"
            textSize = 15f
            setTextColor(0xFF555555.toInt())
        }

        val deleteButton = Button(requireContext()).apply {
            text = "Elimina"
            setTextColor(resources.getColor(R.color.emergency_red, null))
            setOnClickListener {
                repository.deleteAvailability(
                    availabilityId = availability.id,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Disponibilità eliminata", Toast.LENGTH_SHORT).show()
                        loadAvailabilities()
                    },
                    onError = {
                        Toast.makeText(requireContext(), "Errore eliminazione", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }

        card.addView(title)
        card.addView(subtitle)
        card.addView(deleteButton)

        return card
    }

    private fun createUrgentRequestCard(request: UrgentRequestDto): View {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 18, 22, 18)
            background = ContextCompatCompat.getDrawableSafe(this@PsychologistDashboardFragment, R.drawable.bg_gray_button)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 14)
            layoutParams = params
        }

        val title = TextView(requireContext()).apply {
            text = request.userName
            textSize = 18f
            setTextColor(resources.getColor(R.color.black, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val subtitle = TextView(requireContext()).apply {
            text = "Richiesta urgente in attesa"
            textSize = 15f
            setTextColor(0xFF555555.toInt())
        }

        val acceptButton = Button(requireContext()).apply {
            text = "Accetta richiesta"
            setOnClickListener {
                repository.createChatFromUrgentRequest(
                    request = request,
                    psychologistId = sessionManager.getUserId(),
                    psychologistName = sessionManager.getName(),
                    onSuccess = { chatId ->
                        Toast.makeText(requireContext(), "Chat avviata", Toast.LENGTH_SHORT).show()

                        val bundle = Bundle().apply {
                            putString("chatId", chatId)
                        }

                        findNavController().navigate(R.id.chatRoomFragment, bundle)
                    },
                    onError = { exception ->
                        Toast.makeText(
                            requireContext(),
                            exception.message ?: "Errore avvio chat",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }

        card.addView(title)
        card.addView(subtitle)
        card.addView(acceptButton)

        return card
    }

    private fun createEmptyText(message: String): TextView {
        return TextView(requireContext()).apply {
            text = message
            textSize = 15f
            setTextColor(0xFF555555.toInt())
            setPadding(0, 10, 0, 18)
        }
    }
}

private object ContextCompatCompat {
    fun getDrawableSafe(fragment: Fragment, drawableId: Int) =
        androidx.core.content.ContextCompat.getDrawable(fragment.requireContext(), drawableId)
}