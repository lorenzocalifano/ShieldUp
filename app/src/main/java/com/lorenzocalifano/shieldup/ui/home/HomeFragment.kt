package com.lorenzocalifano.shieldup.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.lorenzocalifano.shieldup.R

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnContacts).setOnClickListener {
            findNavController().navigate(R.id.contactsFragment)
        }

        view.findViewById<Button>(R.id.btnSupport).setOnClickListener {
            findNavController().navigate(R.id.supportFragment)
        }

        view.findViewById<Button>(R.id.btnReport).setOnClickListener {
            findNavController().navigate(R.id.redZonesFragment)
        }
    }
}