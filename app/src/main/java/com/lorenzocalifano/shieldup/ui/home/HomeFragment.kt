package com.lorenzocalifano.shieldup.ui.home

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.lorenzocalifano.shieldup.ui.fakecall.FakeCallNotification
import com.lorenzocalifano.shieldup.R
import com.lorenzocalifano.shieldup.utils.SessionManager
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

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
            val bundle = Bundle().apply {
                putBoolean("followMeMode", true)
            }

            findNavController().navigate(
                R.id.redZonesFragment,
                bundle
            )
        }

        view.findViewById<TextView>(R.id.cardSafeRouteCalculator).setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("safeRouteMode", true)
            }

            findNavController().navigate(R.id.redZonesFragment, bundle)
        }

        view.findViewById<TextView>(R.id.cardFakeCall).setOnClickListener {
            showFakeCallDialog()
        }
    }

    private fun showFakeCallDialog() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                requireContext(),
                "Consenti le notifiche per usare la chiamata simulata",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 0)
        }

        val callerInput = EditText(requireContext()).apply {
            hint = "Nome da visualizzare"
            setText("Contatto fidato")
            setSingleLine(true)
        }

        container.addView(callerInput)

        val delays = arrayOf("Tra 10 secondi", "Tra 30 secondi", "Tra 60 secondi")

        AlertDialog.Builder(requireContext())
            .setTitle("Chiamata simulata")
            .setView(container)
            .setSingleChoiceItems(delays, 0, null)
            .setNegativeButton("Annulla", null)
            .setPositiveButton("Programma") { dialog, _ ->
                val selectedPosition = (dialog as AlertDialog).listView.checkedItemPosition

                val delayMillis = when (selectedPosition) {
                    0 -> 10_000L
                    1 -> 30_000L
                    else -> 60_000L
                }

                val callerName = callerInput.text.toString().trim().ifEmpty {
                    "Contatto fidato"
                }

                scheduleFakeCall(callerName, delayMillis)

                Toast.makeText(
                    requireContext(),
                    "Chiamata simulata programmata",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    private fun scheduleFakeCall(callerName: String, delayMillis: Long) {
        /*
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isAdded) return@postDelayed

            FakeCallNotification.showIncomingCallNotification(
                context = requireContext(),
                callerName = callerName
            )
        }, delayMillis)*/
        Handler(Looper.getMainLooper()).postDelayed({
            FakeCallNotification.showIncomingCallNotification(
                requireContext(),
                "Contatto fidato"
            )
        }, 5000)
    }
}