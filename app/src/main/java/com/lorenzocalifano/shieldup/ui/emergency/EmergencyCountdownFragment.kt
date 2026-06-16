package com.lorenzocalifano.shieldup.ui.emergency

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.lorenzocalifano.shieldup.R

class EmergencyCountdownFragment : Fragment(R.layout.fragment_emergency_countdown) {

    private var timer: CountDownTimer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val txtCountdown = view.findViewById<TextView>(R.id.txtCountdown)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelEmergency)

        timer = object : CountDownTimer(10_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                txtCountdown.text = "00:%02d".format(seconds)
            }

            override fun onFinish() {
                Toast.makeText(requireContext(), "Chiamata al 112 simulata", Toast.LENGTH_LONG).show()
                findNavController().popBackStack(R.id.homeFragment, false)
            }
        }.start()

        btnCancel.setOnClickListener {
            timer?.cancel()
            findNavController().popBackStack(R.id.homeFragment, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
    }
}