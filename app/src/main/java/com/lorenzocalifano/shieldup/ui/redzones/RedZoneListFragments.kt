package com.lorenzocalifano.shieldup.ui.redzones

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.lorenzocalifano.shieldup.R
import com.lorenzocalifano.shieldup.data.FirebaseRepository
import com.lorenzocalifano.shieldup.data.RedZoneDto

class RedZoneListFragment : Fragment(R.layout.fragment_red_zone_list) {

    private val repository = FirebaseRepository()
    private val redZoneDurationMs = 60L * 60L * 1000L
    private val recentDurationMs = 3L * 24L * 60L * 60L * 1000L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activeContainer = view.findViewById<LinearLayout>(R.id.activeContainer)
        val recentContainer = view.findViewById<LinearLayout>(R.id.recentContainer)

        view.findViewById<TextView>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }

        loadRedZonesFromFirestore(activeContainer, recentContainer)
    }

    private fun loadRedZonesFromFirestore(
        activeContainer: LinearLayout,
        recentContainer: LinearLayout
    ) {
        repository.loadRedZones(
            onSuccess = { zones ->
                val now = System.currentTimeMillis()

                val activeZones = zones.filter {
                    now - it.createdAt < redZoneDurationMs
                }

                val recentZones = zones.filter {
                    now - it.createdAt < recentDurationMs
                }

                fillContainer(
                    container = activeContainer,
                    items = activeZones,
                    emptyText = "Nessuna segnalazione attiva"
                )

                fillContainer(
                    container = recentContainer,
                    items = recentZones,
                    emptyText = "Nessuna segnalazione recente"
                )
            },
            onError = { exception ->
                Toast.makeText(
                    requireContext(),
                    exception.message ?: "Errore caricamento segnalazioni",
                    Toast.LENGTH_LONG
                ).show()

                fillContainer(activeContainer, emptyList(), "Errore caricamento")
                fillContainer(recentContainer, emptyList(), "Errore caricamento")
            }
        )
    }

    private fun fillContainer(
        container: LinearLayout,
        items: List<RedZoneDto>,
        emptyText: String
    ) {
        container.removeAllViews()

        if (items.isEmpty()) {
            val empty = TextView(requireContext())
            empty.text = emptyText
            empty.textSize = 16f
            empty.setPadding(0, 20, 0, 8)
            container.addView(empty)
            return
        }

        items.forEach { item ->
            container.addView(createCard(item))
        }
    }

    private fun createCard(item: RedZoneDto): TextView {
        val now = System.currentTimeMillis()
        val minutesLeft = ((redZoneDurationMs - (now - item.createdAt)) / 60000)
            .coerceAtLeast(0)

        val statusText = if (minutesLeft > 0) {
            "Scade tra circa $minutesLeft min"
        } else {
            "Segnalazione recente non più attiva"
        }

        val textView = TextView(requireContext())
        textView.text = "${item.title}\n${item.description}\n$statusText"
        textView.textSize = 16f
        textView.setTextColor(resources.getColor(R.color.black, null))
        textView.setBackgroundResource(R.drawable.bg_gray_button)
        textView.setPadding(24, 20, 24, 20)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 14, 0, 0)
        textView.layoutParams = params

        return textView
    }
}