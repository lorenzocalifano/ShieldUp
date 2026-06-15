package com.lorenzocalifano.shieldup.ui.support

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.lorenzocalifano.shieldup.R

class SupportFragment : Fragment(R.layout.fragment_support) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bookChatButton = view.findViewById<Button>(R.id.btnBookChat)
        val bookVideoButton = view.findViewById<Button>(R.id.btnBookVideo)
        val waitingRoomButton = view.findViewById<Button>(R.id.btnWaitingRoom)

        bookChatButton.setOnClickListener {
            Toast.makeText(requireContext(), "Chat prenotata per oggi alle 15:00", Toast.LENGTH_SHORT).show()
        }

        bookVideoButton.setOnClickListener {
            Toast.makeText(requireContext(), "Videochat prenotata per domani alle 10:00", Toast.LENGTH_SHORT).show()
        }

        waitingRoomButton.setOnClickListener {
            Toast.makeText(requireContext(), "Sala d’attesa simulata", Toast.LENGTH_SHORT).show()
        }
    }
}