package com.lorenzocalifano.shieldup.ui.redzones

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.lorenzocalifano.shieldup.R

class RedZoneListFragment : Fragment(R.layout.fragment_red_zone_list) {

    data class RedZoneItem(
        val title: String,
        val description: String,
        val latitude: Double,
        val longitude: Double,
        val createdAt: Long,
        val expiresAt: Long
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activeContainer = view.findViewById<LinearLayout>(R.id.activeContainer)
        val recentContainer = view.findViewById<LinearLayout>(R.id.recentContainer)

        view.findViewById<TextView>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }

        val zones = loadRedZones()
        val now = System.currentTimeMillis()

        val active = zones.filter { it.expiresAt > now }
        val recent = zones.filter { now - it.createdAt <= 3L * 24L * 60L * 60L * 1000L }

        fillContainer(activeContainer, active, "Nessuna segnalazione attiva")
        fillContainer(recentContainer, recent, "Nessuna segnalazione recente")
    }

    private fun fillContainer(container: LinearLayout, items: List<RedZoneItem>, emptyText: String) {
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

    private fun createCard(item: RedZoneItem): TextView {
        val minutesLeft = ((item.expiresAt - System.currentTimeMillis()) / 60000).coerceAtLeast(0)

        val textView = TextView(requireContext())
        textView.text = "${item.title}\n${item.description}\nScade tra circa $minutesLeft min"
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

    private fun loadRedZones(): List<RedZoneItem> {
        val text = requireContext()
            .getSharedPreferences("shield_red_zones", Context.MODE_PRIVATE)
            .getString("red_zones", "") ?: ""

        if (text.isBlank()) return emptyList()

        return text.split(";;").mapNotNull { row ->
            val parts = row.split("|")
            if (parts.size != 6) return@mapNotNull null

            RedZoneItem(
                title = parts[0],
                description = parts[1],
                latitude = parts[2].toDoubleOrNull() ?: return@mapNotNull null,
                longitude = parts[3].toDoubleOrNull() ?: return@mapNotNull null,
                createdAt = parts[4].toLongOrNull() ?: return@mapNotNull null,
                expiresAt = parts[5].toLongOrNull() ?: return@mapNotNull null
            )
        }
    }
}