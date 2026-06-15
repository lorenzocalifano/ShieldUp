package com.lorenzocalifano.shieldup.ui.redzones

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.lorenzocalifano.shieldup.R

class RedZonesFragment : Fragment(R.layout.fragment_red_zones) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val addButton = view.findViewById<Button>(R.id.btnAddRedZone)
        val listButton = view.findViewById<Button>(R.id.btnRedZoneList)

        addButton.setOnClickListener {
            Toast.makeText(requireContext(), "Aggiunta Red Zone da implementare", Toast.LENGTH_SHORT).show()
        }

        listButton.setOnClickListener {
            Toast.makeText(requireContext(), "Lista Red Zones da implementare", Toast.LENGTH_SHORT).show()
        }
    }
}