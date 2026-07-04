package com.lorenzocalifano.shieldup.ui.psychologist

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.lorenzocalifano.shieldup.R
import com.lorenzocalifano.shieldup.data.AvailabilityDto
import com.lorenzocalifano.shieldup.data.FirebaseRepository
import com.lorenzocalifano.shieldup.data.UrgentRequestDto
import com.lorenzocalifano.shieldup.utils.SessionManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class PsychologistDashboardFragment : Fragment(R.layout.fragment_psychologist_dashboard) {

    private val repository = FirebaseRepository()

    private lateinit var sessionManager: SessionManager
    private lateinit var availabilitiesContainer: LinearLayout
    private lateinit var urgentRequestsContainer: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        view.findViewById<TextView>(R.id.txtPsychologistInfo).text =
            "${sessionManager.getName()}\n${sessionManager.getEmail()}"

        availabilitiesContainer = view.findViewById(R.id.availabilitiesContainer)
        urgentRequestsContainer = view.findViewById(R.id.urgentRequestsContainer)

        view.findViewById<Button>(R.id.btnCreateAvailability).setOnClickListener {
            showCreateAvailabilityDialog()
        }

        loadAvailabilities()
        loadUrgentRequests()
    }

    private fun showCreateAvailabilityDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_availability, null)

        val pickDateButton = dialogView.findViewById<Button>(R.id.btnPickDate)
        val pickTimeButton = dialogView.findViewById<Button>(R.id.btnPickTime)
        val selectedDateText = dialogView.findViewById<TextView>(R.id.txtSelectedDate)
        val selectedTimeText = dialogView.findViewById<TextView>(R.id.txtSelectedTime)

        val calendar = Calendar.getInstance()
        var selectedDate = ""
        var selectedTime = ""
        var selectedScheduledAt = 0L

        pickDateButton.setOnClickListener {
            val now = Calendar.getInstance()

            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, day)

                    selectedDate = "%02d/%02d/%04d".format(day, month + 1, year)
                    selectedDateText.text = selectedDate
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        pickTimeButton.setOnClickListener {
            val now = Calendar.getInstance()

            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)

                    selectedTime = "%02d:%02d".format(hour, minute)
                    selectedTimeText.text = selectedTime
                    selectedScheduledAt = calendar.timeInMillis
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
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

                if (selectedScheduledAt <= System.currentTimeMillis()) {
                    Toast.makeText(requireContext(), "La disponibilità deve essere futura", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val availability = AvailabilityDto(
                    psychologistId = sessionManager.getUserId(),
                    psychologistName = sessionManager.getName(),
                    date = selectedDate,
                    time = selectedTime,
                    scheduledAt = selectedScheduledAt,
                    type = "Chat"
                )

                repository.savePsychologistAvailability(
                    availability = availability,
                    onSuccess = {
                        if (!isAdded) return@savePsychologistAvailability
                        Toast.makeText(requireContext(), "Disponibilità salvata", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadAvailabilities()
                    },
                    onError = {
                        if (!isAdded) return@savePsychologistAvailability
                        Toast.makeText(requireContext(), "Errore salvataggio disponibilità", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }

        dialog.show()
    }

    private fun loadAvailabilities() {
        repository.loadPsychologistAvailabilities(
            psychologistId = sessionManager.getUserId(),
            onSuccess = { availabilities ->
                if (!isAdded) return@loadPsychologistAvailabilities

                availabilitiesContainer.removeAllViews()

                if (availabilities.isEmpty()) {
                    availabilitiesContainer.addView(createEmptyText("Nessuna disponibilità futura"))
                    return@loadPsychologistAvailabilities
                }

                availabilities.forEach {
                    availabilitiesContainer.addView(createAvailabilityCard(it))
                }
            },
            onError = {
                if (!isAdded) return@loadPsychologistAvailabilities
                Toast.makeText(requireContext(), "Errore caricamento disponibilità", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun loadUrgentRequests() {
        repository.loadPendingUrgentRequests(
            onSuccess = { requests ->
                if (!isAdded) return@loadPendingUrgentRequests

                urgentRequestsContainer.removeAllViews()

                if (requests.isEmpty()) {
                    urgentRequestsContainer.addView(createEmptyText("Nessuna richiesta urgente in attesa"))
                    return@loadPendingUrgentRequests
                }

                requests.forEach {
                    urgentRequestsContainer.addView(createUrgentRequestCard(it))
                }
            },
            onError = {
                if (!isAdded) return@loadPendingUrgentRequests
                Toast.makeText(requireContext(), "Errore caricamento richieste urgenti", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun createAvailabilityCard(availability: AvailabilityDto): View {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_gray_button)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(14)) }
        }

        val title = TextView(requireContext()).apply {
            text = "${availability.date} - ${availability.time}"
            textSize = 18f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val subtitle = TextView(requireContext()).apply {
            text = if (availability.booked) "Chat prenotata" else "Chat disponibile"
            textSize = 15f
            setTextColor(0xFF555555.toInt())
            setPadding(0, dp(4), 0, 0)
        }

        val deleteButton = Button(requireContext()).apply {
            text = "Elimina"
            setTextColor(ContextCompat.getColor(requireContext(), R.color.emergency_red))
            setOnClickListener {
                repository.deleteAvailability(
                    availabilityId = availability.id,
                    onSuccess = {
                        if (!isAdded) return@deleteAvailability
                        Toast.makeText(requireContext(), "Disponibilità eliminata", Toast.LENGTH_SHORT).show()
                        loadAvailabilities()
                    },
                    onError = {
                        if (!isAdded) return@deleteAvailability
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
        val minutesWaiting = TimeUnit.MILLISECONDS.toMinutes(
            System.currentTimeMillis() - request.createdAt
        ).coerceAtLeast(0)

        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_gray_button)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(14)) }
        }

        val title = TextView(requireContext()).apply {
            text = request.userName
            textSize = 18f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val subtitle = TextView(requireContext()).apply {
            text = "In attesa da $minutesWaiting minuti"
            textSize = 15f
            setTextColor(0xFF555555.toInt())
            setPadding(0, dp(4), 0, 0)
        }

        val acceptButton = Button(requireContext()).apply {
            text = "Accetta e apri chat"
            setOnClickListener {
                repository.createChatFromUrgentRequest(
                    request = request,
                    psychologistId = sessionManager.getUserId(),
                    psychologistName = sessionManager.getName(),
                    onSuccess = { chatId ->
                        if (!isAdded) return@createChatFromUrgentRequest

                        findNavController().navigate(
                            R.id.chatRoomFragment,
                            Bundle().apply {
                                putString("chatId", chatId)
                                putString("chatTitle", request.userName)
                            }
                        )
                    },
                    onError = { exception ->
                        if (!isAdded) return@createChatFromUrgentRequest
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
            setPadding(0, dp(10), 0, dp(18))
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}