package com.lorenzocalifano.shieldup.ui.emergency

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.telephony.SmsManager
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.lorenzocalifano.shieldup.R

class EmergencyCountdownFragment : Fragment(R.layout.fragment_emergency_countdown) {

    private var timer: CountDownTimer? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val smsGranted = permissions[Manifest.permission.SEND_SMS] == true
            val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (smsGranted && locationGranted) {
                startEmergencyProcedure()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permessi necessari per inviare SOS completi",
                    Toast.LENGTH_LONG
                ).show()
                openDialer()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val txtCountdown = view.findViewById<TextView>(R.id.txtCountdown)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelEmergency)

        timer = object : CountDownTimer(10_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                txtCountdown.text = "00:%02d".format(seconds)
            }

            override fun onFinish() {
                checkPermissionsAndStart()
            }
        }.start()

        btnCancel.setOnClickListener {
            timer?.cancel()
            findNavController().popBackStack(R.id.homeFragment, false)
        }
    }

    private fun checkPermissionsAndStart() {
        val smsGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val fineLocationGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (smsGranted && (fineLocationGranted || coarseLocationGranted)) {
            startEmergencyProcedure()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun startEmergencyProcedure() {
        sendEmergencyMessages()
        openDialer()
        findNavController().popBackStack(R.id.homeFragment, false)
    }

    private fun openDialer() {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:112")
        }
        startActivity(intent)
    }

    private fun sendEmergencyMessages() {
        val contacts = loadEmergencyContacts()

        if (contacts.isEmpty()) {
            Toast.makeText(requireContext(), "Nessun contatto SOS salvato", Toast.LENGTH_LONG).show()
            return
        }

        val location = getLastKnownLocation()
        val message = buildEmergencyMessage(location)

        val smsManager = SmsManager.getDefault()

        contacts.forEach { phone ->
            try {
                smsManager.sendTextMessage(phone, null, message, null, null)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Errore invio SMS a $phone", Toast.LENGTH_SHORT).show()
            }
        }

        Toast.makeText(requireContext(), "SMS SOS inviati", Toast.LENGTH_LONG).show()
    }

    private fun loadEmergencyContacts(): List<String> {
        val sharedPref = requireContext().getSharedPreferences("shield_contacts", Context.MODE_PRIVATE)
        val savedText = sharedPref.getString("contacts", "") ?: ""

        if (savedText.isBlank()) return emptyList()

        return savedText
            .split(";;")
            .mapNotNull { row ->
                val parts = row.split(" - ")
                if (parts.size >= 2) parts[1].trim() else null
            }
            .filter { it.isNotBlank() }
    }

    private fun getLastKnownLocation(): Location? {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val fineGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) return null

        val providers = locationManager.getProviders(true)

        for (provider in providers) {
            val location = locationManager.getLastKnownLocation(provider)
            if (location != null) {
                return location
            }
        }

        return null
    }

    private fun buildEmergencyMessage(location: Location?): String {
        return if (location != null) {
            val lat = location.latitude
            val lon = location.longitude
            val mapLink = "https://maps.google.com/?q=$lat,$lon"

            "SOS ShieldUp: ho bisogno di aiuto. Posizione: $mapLink"
        } else {
            "SOS ShieldUp: ho bisogno di aiuto. Posizione GPS non disponibile."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
    }
}