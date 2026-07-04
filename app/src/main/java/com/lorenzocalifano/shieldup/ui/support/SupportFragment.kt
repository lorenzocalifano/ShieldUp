package com.lorenzocalifano.shieldup.ui.support

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.lorenzocalifano.shieldup.R
import com.lorenzocalifano.shieldup.data.AvailabilityDto
import com.lorenzocalifano.shieldup.data.FirebaseRepository
import com.lorenzocalifano.shieldup.utils.SessionManager

class SupportFragment : Fragment(R.layout.fragment_support) {

    private val repository = FirebaseRepository()

    private lateinit var sessionManager: SessionManager
    private lateinit var slotsContainer: LinearLayout
    private lateinit var urgentHelpButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        slotsContainer = view.findViewById(R.id.slotsContainer)
        urgentHelpButton = view.findViewById(R.id.btnUrgentHelp)

        urgentHelpButton.setOnClickListener {
            createUrgentRequest()
        }

        view.findViewById<Button>(R.id.btnOtherReports).setOnClickListener {
            Toast.makeText(requireContext(), "Sezione altre segnalazioni da implementare", Toast.LENGTH_SHORT).show()
        }

        loadAvailableSlots()
    }

    override fun onResume() {
        super.onResume()
        if (::slotsContainer.isInitialized) {
            loadAvailableSlots()
        }
    }

    private fun loadAvailableSlots() {
        slotsContainer.removeAllViews()
        slotsContainer.addView(createInfoText("Caricamento disponibilità..."))

        repository.loadAvailableSlots(
            onSuccess = { slots ->
                if (!isAdded) return@loadAvailableSlots

                slotsContainer.removeAllViews()

                if (slots.isEmpty()) {
                    slotsContainer.addView(createInfoText("Al momento non ci sono disponibilità prenotabili."))
                    return@loadAvailableSlots
                }

                slots.forEach { slot ->
                    slotsContainer.addView(createSlotCard(slot))
                }
            },
            onError = {
                if (!isAdded) return@loadAvailableSlots
                slotsContainer.removeAllViews()
                slotsContainer.addView(createInfoText("Errore nel caricamento delle disponibilità."))
            }
        )
    }

    private fun createUrgentRequest() {
        urgentHelpButton.isEnabled = false
        urgentHelpButton.text = "Richiesta in invio..."

        repository.createUrgentPsychologicalRequest(
            userId = sessionManager.getUserId(),
            userName = sessionManager.getName(),
            onSuccess = {
                if (!isAdded) return@createUrgentPsychologicalRequest

                urgentHelpButton.isEnabled = true
                urgentHelpButton.text = "Ho bisogno di aiuto subito"

                Toast.makeText(
                    requireContext(),
                    "Richiesta inviata. Rimani in attesa di uno psicologo.",
                    Toast.LENGTH_LONG
                ).show()
            },
            onError = { exception ->
                if (!isAdded) return@createUrgentPsychologicalRequest

                urgentHelpButton.isEnabled = true
                urgentHelpButton.text = "Ho bisogno di aiuto subito"

                Toast.makeText(
                    requireContext(),
                    exception.message ?: "Errore invio richiesta urgente",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun createSlotCard(slot: AvailabilityDto): View {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(14), dp(12), dp(14))
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_gray_button)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dp(14))
            }
        }

        val psychologistName = TextView(requireContext()).apply {
            text = slot.psychologistName
            textSize = 22f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val details = TextView(requireContext()).apply {
            text = "${slot.date} alle ${slot.time}\nTipo consulto: ${slot.type}"
            textSize = 18f
            setTextColor(0xFF555555.toInt())
            setPadding(0, dp(6), 0, 0)
        }

        val bookButton = Button(requireContext()).apply {
            text = "Prenota"
            textSize = 17f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_purple_button)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                setMargins(0, dp(14), 0, 0)
            }

            setOnClickListener {
                bookSlot(slot, this)
            }
        }

        card.addView(psychologistName)
        card.addView(details)
        card.addView(bookButton)

        return card
    }

    private fun bookSlot(slot: AvailabilityDto, button: Button) {
        button.isEnabled = false
        button.text = "Prenotazione..."

        repository.bookAvailability(
            availabilityId = slot.id,
            userId = sessionManager.getUserId(),
            userName = sessionManager.getName(),
            onSuccess = {
                if (!isAdded) return@bookAvailability

                Toast.makeText(
                    requireContext(),
                    "Prenotazione confermata con ${slot.psychologistName}",
                    Toast.LENGTH_LONG
                ).show()

                loadAvailableSlots()
            },
            onError = { exception ->
                if (!isAdded) return@bookAvailability

                button.isEnabled = true
                button.text = "Prenota"

                Toast.makeText(
                    requireContext(),
                    exception.message ?: "Errore durante la prenotazione",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun createInfoText(message: String): TextView {
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