package com.lorenzocalifano.shieldup.ui.home

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.lorenzocalifano.shieldup.R
import com.lorenzocalifano.shieldup.utils.SessionManager

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())
        val firstName = sessionManager.getName().trim().split(" ").firstOrNull().orEmpty()

        view.findViewById<TextView>(R.id.txtWelcome).text =
            if (firstName.isNotEmpty()) "Ciao $firstName" else "Ciao"

        view.findViewById<TextView>(R.id.cardEmergency).setOnClickListener {
            findNavController().navigate(R.id.emergencyCountdownFragment)
        }

        view.findViewById<TextView>(R.id.cardReport).setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("openReportDialog", true)
            }

            findNavController().navigate(R.id.redZonesFragment, bundle)
        }

        view.findViewById<TextView>(R.id.cardSafeRoute).setOnClickListener {
            findNavController().navigate(R.id.emergencyCountdownFragment)
        }
    }
}