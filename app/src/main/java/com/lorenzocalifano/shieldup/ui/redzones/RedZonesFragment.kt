package com.lorenzocalifano.shieldup.ui.redzones

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.lorenzocalifano.shieldup.R

class RedZonesFragment : Fragment(R.layout.fragment_red_zones) {

    private var googleMap: GoogleMap? = null
    private val anconaCenter = LatLng(43.6158, 13.5189)
    private val redZones = mutableListOf<RedZoneItem>()

    data class RedZoneItem(
        val title: String,
        val description: String,
        val latitude: Double,
        val longitude: Double,
        val createdAt: Long,
        val expiresAt: Long
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadRedZones()
        setupMap()
        setupButtons(view)
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.googleMap) as SupportMapFragment

        mapFragment.getMapAsync { map ->
            googleMap = map

            map.uiSettings.isZoomControlsEnabled = false
            map.uiSettings.isMyLocationButtonEnabled = false

            enableUserLocationIfAllowed()
            showSavedRedZones()
            moveToCurrentLocation()
        }
    }

    private fun setupButtons(view: View) {
        view.findViewById<Button>(R.id.btnAddRedZone).setOnClickListener {
            showAddRedZoneDialog()
        }

        view.findViewById<Button>(R.id.btnRedZoneList).setOnClickListener {
            findNavController().navigate(R.id.redZoneListFragment)
        }

        view.findViewById<Button>(R.id.btnMyLocation).setOnClickListener {
            moveToCurrentLocation()
        }
    }

    private fun showAddRedZoneDialog() {
        val container = LinearLayout(requireContext())
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(48, 24, 48, 8)

        val titleInput = EditText(requireContext())
        titleInput.hint = "Titolo segnalazione"
        titleInput.textSize = 16f

        val descriptionInput = EditText(requireContext())
        descriptionInput.hint = "Descrizione"
        descriptionInput.textSize = 16f
        descriptionInput.minLines = 2

        val positionSwitch = Switch(requireContext())
        positionSwitch.text = "Usa la mia posizione attuale"
        positionSwitch.textSize = 16f
        positionSwitch.isChecked = true
        positionSwitch.setPadding(0, 24, 0, 8)

        val infoText = TextView(requireContext())
        infoText.text =
            "Se disattivi questa opzione, la segnalazione verrà inserita nel centro della mappa. Sposta la mappa sul punto da segnalare prima di salvare."
        infoText.textSize = 14f
        infoText.setTextColor(Color.DKGRAY)
        infoText.setPadding(0, 8, 0, 0)

        positionSwitch.setOnCheckedChangeListener { _, isChecked ->
            infoText.text = if (isChecked) {
                "La segnalazione verrà inserita nella tua posizione GPS attuale."
            } else {
                "La segnalazione verrà inserita nel centro della mappa. Sposta la mappa sul punto da segnalare prima di salvare."
            }
        }

        container.addView(titleInput)
        container.addView(descriptionInput)
        container.addView(positionSwitch)
        container.addView(infoText)

        AlertDialog.Builder(requireContext())
            .setTitle("Nuova Red Zone")
            .setView(container)
            .setNegativeButton("Annulla", null)
            .setPositiveButton("Salva", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val title = titleInput.text.toString().trim()
                        val description = descriptionInput.text.toString().trim()

                        if (title.isEmpty() || description.isEmpty()) {
                            Toast.makeText(
                                requireContext(),
                                "Compila tutti i campi",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        if (positionSwitch.isChecked) {
                            saveUsingCurrentPosition(title, description)
                        } else {
                            saveUsingMapCenter(title, description)
                        }

                        dismiss()
                    }
                }
            }
            .show()
    }

    private fun saveUsingCurrentPosition(title: String, description: String) {
        if (!hasLocationPermission()) {
            Toast.makeText(
                requireContext(),
                "Permesso posizione non concesso. Uso il centro della mappa.",
                Toast.LENGTH_LONG
            ).show()
            saveUsingMapCenter(title, description)
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                val position = if (location != null) {
                    LatLng(location.latitude, location.longitude)
                } else {
                    googleMap?.cameraPosition?.target ?: anconaCenter
                }

                saveRedZone(title, description, position)
            }
            .addOnFailureListener {
                val position = googleMap?.cameraPosition?.target ?: anconaCenter
                saveRedZone(title, description, position)
            }
    }

    private fun saveUsingMapCenter(title: String, description: String) {
        val position = googleMap?.cameraPosition?.target ?: anconaCenter
        saveRedZone(title, description, position)
    }

    private fun saveRedZone(title: String, description: String, position: LatLng) {
        val now = System.currentTimeMillis()
        val expiresAt = now + 60L * 60L * 1000L

        val redZone = RedZoneItem(
            title = title,
            description = description,
            latitude = position.latitude,
            longitude = position.longitude,
            createdAt = now,
            expiresAt = expiresAt
        )

        redZones.add(redZone)
        saveRedZones()
        addRedZoneToMap(redZone)

        Toast.makeText(requireContext(), "Segnalazione salvata", Toast.LENGTH_SHORT).show()
    }

    private fun addRedZoneToMap(redZone: RedZoneItem) {
        val map = googleMap ?: return
        val position = LatLng(redZone.latitude, redZone.longitude)

        map.addMarker(
            MarkerOptions()
                .position(position)
                .title(redZone.title)
                .snippet(redZone.description)
        )

        map.addCircle(
            CircleOptions()
                .center(position)
                .radius(160.0)
                .strokeColor(0x99FF0000.toInt())
                .fillColor(0x33FF0000)
                .strokeWidth(4f)
        )
    }

    private fun showSavedRedZones() {
        val now = System.currentTimeMillis()

        redZones
            .filter { it.expiresAt > now }
            .forEach { addRedZoneToMap(it) }
    }

    private fun saveRedZones() {
        val text = redZones.joinToString(";;") {
            "${it.title}|${it.description}|${it.latitude}|${it.longitude}|${it.createdAt}|${it.expiresAt}"
        }

        requireContext()
            .getSharedPreferences("shield_red_zones", Context.MODE_PRIVATE)
            .edit()
            .putString("red_zones", text)
            .apply()
    }

    private fun loadRedZones() {
        val text = requireContext()
            .getSharedPreferences("shield_red_zones", Context.MODE_PRIVATE)
            .getString("red_zones", "") ?: ""

        redZones.clear()

        if (text.isBlank()) return

        text.split(";;").forEach { row ->
            val parts = row.split("|")

            if (parts.size == 6) {
                val latitude = parts[2].toDoubleOrNull()
                val longitude = parts[3].toDoubleOrNull()
                val createdAt = parts[4].toLongOrNull()
                val expiresAt = parts[5].toLongOrNull()

                if (latitude != null && longitude != null && createdAt != null && expiresAt != null) {
                    redZones.add(
                        RedZoneItem(
                            title = parts[0],
                            description = parts[1],
                            latitude = latitude,
                            longitude = longitude,
                            createdAt = createdAt,
                            expiresAt = expiresAt
                        )
                    )
                }
            }
        }
    }

    private fun enableUserLocationIfAllowed() {
        if (hasLocationPermission()) {
            googleMap?.isMyLocationEnabled = true
        }
    }

    private fun moveToCurrentLocation() {
        if (!hasLocationPermission()) {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(anconaCenter, 14f))
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val currentPosition = LatLng(location.latitude, location.longitude)
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(currentPosition, 16f)
                    )
                } else {
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(anconaCenter, 14f)
                    )
                }
            }
            .addOnFailureListener {
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(anconaCenter, 14f))
            }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }
}