package com.lorenzocalifano.shieldup.ui.redzones

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import com.lorenzocalifano.shieldup.utils.SessionManager

class RedZonesFragment : Fragment(R.layout.fragment_red_zones) {

    private val repository = FirebaseRepository()
    private var googleMap: GoogleMap? = null

    private val anconaCenter = LatLng(43.6158, 13.5189)
    private val redZoneDurationMs = 60L * 60L * 1000L

    companion object {
        private const val TAG = "SHIELDUP_MAP"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "onViewCreated chiamato")

        setupMap()
        setupButtons(view)

        if (arguments?.getBoolean("openReportDialog") == true) {
            view.postDelayed({
                if (isAdded) {
                    showAddRedZoneDialog()
                    arguments?.remove("openReportDialog")
                }
            }, 500)
        }
    }

    private fun setupMap() {
        Log.d(TAG, "setupMap avviato")

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.googleMap) as? SupportMapFragment

        if (mapFragment == null) {
            Log.e(TAG, "SupportMapFragment NON trovato. Controlla fragment_red_zones.xml")
            Toast.makeText(requireContext(), "Errore caricamento mappa", Toast.LENGTH_LONG).show()
            return
        }

        Log.d(TAG, "SupportMapFragment trovato")

        mapFragment.getMapAsync { map ->
            Log.d(TAG, "onMapReady chiamato")

            googleMap = map
            Log.d(TAG, "GoogleMap assegnata correttamente")

            map.uiSettings.isZoomControlsEnabled = false
            map.uiSettings.isMyLocationButtonEnabled = false

            Log.d(TAG, "Permesso posizione: ${hasLocationPermission()}")

            enableUserLocationIfAllowed()

            Log.d(TAG, "Sposto camera su posizione corrente o Ancona")
            moveToCurrentLocation()

            Log.d(TAG, "Inizio caricamento Red Zones da Firestore")
            loadRedZonesFromFirebase()
        }
    }

    private fun setupButtons(view: View) {
        Log.d(TAG, "setupButtons avviato")

        view.findViewById<Button>(R.id.btnAddRedZone).setOnClickListener {
            Log.d(TAG, "Click btnAddRedZone")
            showAddRedZoneDialog()
        }

        view.findViewById<Button>(R.id.btnRedZoneList).setOnClickListener {
            Log.d(TAG, "Click btnRedZoneList")
            findNavController().navigate(R.id.redZoneListFragment)
        }

        view.findViewById<Button>(R.id.btnMyLocation).setOnClickListener {
            Log.d(TAG, "Click btnMyLocation")
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
            Log.w(TAG, "Permesso posizione non concesso, uso centro mappa")
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
                    Log.d(TAG, "Posizione GPS ottenuta: ${location.latitude}, ${location.longitude}")
                    LatLng(location.latitude, location.longitude)
                } else {
                    Log.w(TAG, "lastLocation null, uso centro mappa")
                    googleMap?.cameraPosition?.target ?: anconaCenter
                }

                saveRedZoneToFirebase(title, description, position)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Errore recupero posizione", exception)
                val position = googleMap?.cameraPosition?.target ?: anconaCenter
                saveRedZoneToFirebase(title, description, position)
            }
    }

    private fun saveUsingMapCenter(title: String, description: String) {
        val position = googleMap?.cameraPosition?.target ?: anconaCenter
        Log.d(TAG, "Salvo usando centro mappa: ${position.latitude}, ${position.longitude}")
        saveRedZoneToFirebase(title, description, position)
    }

    private fun saveRedZoneToFirebase(title: String, description: String, position: LatLng) {
        val sessionManager = SessionManager(requireContext())
        val currentUserId = sessionManager.getUserId().ifBlank { "demo_user" }

        val redZone = RedZoneDto(
            title = title,
            description = description,
            latitude = position.latitude,
            longitude = position.longitude,
            createdAt = System.currentTimeMillis(),
            userId = currentUserId,
            type = "Pericolo"
        )

        Log.d(TAG, "Salvataggio Red Zone su Firebase: $redZone")

        repository.saveRedZone(
            redZone = redZone,
            onSuccess = {
                Log.d(TAG, "Red Zone salvata correttamente")
                Toast.makeText(requireContext(), "Segnalazione salvata", Toast.LENGTH_SHORT).show()
                googleMap?.clear()
                loadRedZonesFromFirebase()
            },
            onError = { exception ->
                Log.e(TAG, "Errore salvataggio Red Zone", exception)
                Toast.makeText(
                    requireContext(),
                    exception.message ?: "Errore salvataggio Red Zone",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun loadRedZonesFromFirebase() {
        Log.d(TAG, "loadRedZonesFromFirebase chiamato")

        repository.loadRedZones(
            onSuccess = { zones ->
                if (!isAdded || context == null) return@loadRedZones
                val now = System.currentTimeMillis()

                Log.d(TAG, "Red Zones ricevute da Firestore: ${zones.size}")

                googleMap?.clear()
                enableUserLocationIfAllowed()

                val activeZones = zones.filter { now - it.createdAt < redZoneDurationMs }

                Log.d(TAG, "Red Zones attive da mostrare: ${activeZones.size}")

                activeZones.forEach { zone ->
                    Log.d(TAG, "Aggiungo marker: ${zone.title} ${zone.latitude}, ${zone.longitude}")
                    addRedZoneToMap(zone)
                }

                if (activeZones.isEmpty()) {
                    Log.d(TAG, "Nessuna Red Zone attiva")
                }
            },
            onError = { exception ->
                if (!isAdded || context == null) return@loadRedZones
                Log.e(TAG, "Errore caricamento Red Zones da Firestore", exception)
                Toast.makeText(
                    requireContext(),
                    exception.message ?: "Errore caricamento Red Zones",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun addRedZoneToMap(redZone: RedZoneDto) {
        val map = googleMap

        if (map == null) {
            Log.e(TAG, "addRedZoneToMap chiamato ma googleMap è null")
            return
        }

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
        if (!isAdded || context == null) return

        if (hasLocationPermission()) {
            try {
                googleMap?.isMyLocationEnabled = true
            } catch (exception: SecurityException) {
                Log.e(TAG, "SecurityException su isMyLocationEnabled", exception)
            }
        }
    }

    private fun moveToCurrentLocation() {
        if (!hasLocationPermission()) {
            Log.w(TAG, "No location permission, camera su Ancona")
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(anconaCenter, 14f))
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG, "Camera su posizione corrente: ${location.latitude}, ${location.longitude}")

                    val currentPosition = LatLng(location.latitude, location.longitude)
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(currentPosition, 16f)
                    )
                } else {
                    Log.w(TAG, "lastLocation null, camera su Ancona")
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(anconaCenter, 14f)
                    )
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Errore spostamento camera su posizione corrente", exception)
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