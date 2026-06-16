package com.lorenzocalifano.shieldup.ui.redzones

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
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
import com.lorenzocalifano.shieldup.data.FirebaseRepository
import com.lorenzocalifano.shieldup.data.RedZoneDto

class RedZonesFragment : Fragment(R.layout.fragment_red_zones) {

    private val repository = FirebaseRepository()
    private val currentUserId = "demo_user"

    private var googleMap: GoogleMap? = null
    private val anconaCenter = LatLng(43.6158, 13.5189)

    private val redZoneDurationMs = 60L * 60L * 1000L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            moveToCurrentLocation()
            loadRedZonesFromFirebase()
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
        val dialogView = layoutInflater.inflate(R.layout.dialog_red_zone, null)

        val titleInput = dialogView.findViewById<EditText>(R.id.etRedZoneTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.etRedZoneDescription)
        val positionSwitch = dialogView.findViewById<Switch>(R.id.switchCurrentLocation)
        val infoText = dialogView.findViewById<TextView>(R.id.txtLocationInfo)

        positionSwitch.isChecked = true
        infoText.text = "La segnalazione verrà inserita nella tua posizione GPS attuale."

        positionSwitch.setOnCheckedChangeListener { _, isChecked ->
            infoText.text = if (isChecked) {
                "La segnalazione verrà inserita nella tua posizione GPS attuale."
            } else {
                "La segnalazione verrà inserita nel centro della mappa. Sposta la mappa sul punto da segnalare prima di salvare."
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setNegativeButton("Annulla", null)
            .setPositiveButton("Salva", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = titleInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()

                if (title.isEmpty() || description.isEmpty()) {
                    Toast.makeText(requireContext(), "Compila tutti i campi", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (positionSwitch.isChecked) {
                    saveUsingCurrentPosition(title, description)
                } else {
                    saveUsingMapCenter(title, description)
                }

                dialog.dismiss()
            }
        }

        dialog.show()
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

                saveRedZoneToFirebase(title, description, position)
            }
            .addOnFailureListener {
                val position = googleMap?.cameraPosition?.target ?: anconaCenter
                saveRedZoneToFirebase(title, description, position)
            }
    }

    private fun saveUsingMapCenter(title: String, description: String) {
        val position = googleMap?.cameraPosition?.target ?: anconaCenter
        saveRedZoneToFirebase(title, description, position)
    }

    private fun saveRedZoneToFirebase(title: String, description: String, position: LatLng) {
        val redZone = RedZoneDto(
            title = title,
            description = description,
            latitude = position.latitude,
            longitude = position.longitude,
            createdAt = System.currentTimeMillis(),
            userId = currentUserId,
            type = "Pericolo"
        )

        repository.saveRedZone(
            redZone = redZone,
            onSuccess = {
                Toast.makeText(requireContext(), "Segnalazione salvata", Toast.LENGTH_SHORT).show()
                googleMap?.clear()
                loadRedZonesFromFirebase()
            },
            onError = { exception ->
                Toast.makeText(
                    requireContext(),
                    exception.message ?: "Errore salvataggio Red Zone",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun loadRedZonesFromFirebase() {
        repository.loadRedZones(
            onSuccess = { zones ->
                val now = System.currentTimeMillis()

                googleMap?.clear()
                enableUserLocationIfAllowed()

                zones
                    .filter { now - it.createdAt < redZoneDurationMs }
                    .forEach { addRedZoneToMap(it) }
            },
            onError = { exception ->
                Toast.makeText(
                    requireContext(),
                    exception.message ?: "Errore caricamento Red Zones",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun addRedZoneToMap(redZone: RedZoneDto) {
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